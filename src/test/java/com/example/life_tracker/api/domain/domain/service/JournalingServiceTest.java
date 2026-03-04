package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.ReplyMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JournalingServiceTest {

    @Mock private SessionManagerService sessionManager;
    @Mock private ChatService chatService;
    @Mock private JournalingIngestionService ingestionService;
    @Mock private SseNotificationService sseNotificationService;

    @InjectMocks
    private JournalingService journalingService;

    private final UUID userId = UUID.randomUUID();

    private static final int MAX_INTERACTIONS_BEFORE_SAVE = 3;

    @BeforeEach
    void setup() {
        // Inicializa callbacks (simula o @PostConstruct)
        journalingService.init();
    }

    @Test
    @DisplayName("Deve bloquear usuário se já tem entrada hoje e não tem sessão ativa")
    void shouldBlockUserIfJournalExistsToday() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(false);
        when(ingestionService.hasJournaledToday(userId)).thenReturn(true);

        Flux<String> response = journalingService.handleUserMessage(userId, "Oi");

        StepVerifier.create(response)
                .expectNext(ReplyMessages.JOURNAL_ALREADY_EXISTS)
                .verifyComplete();

        verify(chatService, never()).processMessage(any(), any());
        verify(sessionManager, never()).keepAlive(userId);
    }

    @Test
    @DisplayName("Deve permitir conversa se usuário já tem sessão ativa (mesmo com entrada no banco)")
    void shouldAllowIfSessionIsActive() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getCurrentHistorySize(userId)).thenReturn(0);
        when(chatService.processMessage(any(), any())).thenReturn(Flux.just("Resposta da IA"));

        Flux<String> response = journalingService.handleUserMessage(userId, "Continuando...");

        StepVerifier.create(response)
                .expectNext("Resposta da IA")
                .verifyComplete();

        verify(sessionManager).keepAlive(userId);
    }

    @Test
    @DisplayName("Deve permitir a conversa se não existe entrada no banco")
    void shouldAllowUserIfJournalDoesntExistToday() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(false);
        when(ingestionService.hasJournaledToday(userId)).thenReturn(false);

        when(chatService.processMessage(eq(userId), anyString()))
                .thenReturn(Flux.just("Resposta da IA"));

        Flux<String> response = journalingService.handleUserMessage(userId, "Oi");

        StepVerifier.create(response)
                .expectNext("Resposta da IA")
                .verifyComplete();

        verify(sessionManager).keepAlive(userId);
    }

    @Test
    @DisplayName("Deve encerrar automaticamente quando atingir o limite de mensagens")
    void shouldAutoConsolidateWhenLimitReached() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);

        int limitInMessages = MAX_INTERACTIONS_BEFORE_SAVE * 2;
        when(chatService.getCurrentHistorySize(userId)).thenReturn(limitInMessages);

        when(chatService.getHistorySnapshot(userId)).thenReturn("Histórico Completo");

        Flux<String> response = journalingService.handleUserMessage(userId, "Mais uma msg");

        StepVerifier.create(response)
                .expectNext(ReplyMessages.CONSOLIDATE)
                .verifyComplete();

        // Verifica se chamou a consolidação e limpou tudo
        verify(ingestionService).ingest(anyString(), eq(userId));
        verify(chatService).clearHistory(userId);
        verify(sessionManager).invalidate(userId);
        verify(sseNotificationService).removeEmitter(userId); // Adicionado para garantir limpeza SSE
    }

    @Test
    @DisplayName("Não deve encerrar automaticamente quando estiver abaixo do limite de mensagens")
    void shouldNotAutoConsolidateWhenLimitNotReached() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getCurrentHistorySize(userId)).thenReturn(1);

        // CORREÇÃO DO NPE: Ensinamos o Mock a responder para o fluxo normal
        when(chatService.processMessage(eq(userId), anyString()))
                .thenReturn(Flux.just("Resposta da IA"));

        Flux<String> response = journalingService.handleUserMessage(userId, "Mais uma msg");

        StepVerifier.create(response)
                .expectNext("Resposta da IA")
                .verifyComplete();

        verify(ingestionService, never()).ingest(anyString(), eq(userId));
        verify(chatService, never()).clearHistory(userId);
        verify(chatService).processMessage(eq(userId), anyString());
    }

    @Test
    @DisplayName("Deve consolidar e encerrar manualmente quando solicitado pelo usuário")
    void shouldManuallyConsolidateWhenRequested() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getHistorySnapshot(userId)).thenReturn("Histórico para Salvar");

        Flux<String> response = journalingService.manuallyCloseConversation(userId);

        StepVerifier.create(response)
                .expectNext(ReplyMessages.MANUALLY_CONSOLIDATE)
                .verifyComplete();

        verify(ingestionService).ingest("Histórico para Salvar", userId);
        verify(chatService).clearHistory(userId);
        verify(sessionManager).invalidate(userId);
        verify(sseNotificationService).removeEmitter(userId);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar encerrar manualmente sem sessão ativa")
    void shouldThrowErrorWhenManualClosingWithoutSession() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(false);

        // Verifica o Fail Fast
        assertThrows(IllegalStateException.class, () ->
                journalingService.manuallyCloseConversation(userId)
        );

        // Garante que não fez consolidação indevida
        verify(ingestionService, never()).ingest(anyString(), any());
        verify(chatService, never()).clearHistory(any());
        verify(sessionManager, never()).invalidate(any());
    }

    @Test
    @DisplayName("Deve registrar os callbacks no SessionManager ao inicializar")
    void shouldRegisterCallbacksOnInit() {
        verify(sessionManager).setCallbacks(any(), any());
    }
}