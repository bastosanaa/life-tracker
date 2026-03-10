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
        // Initialize callbacks (simulates @PostConstruct)
        journalingService.init();
    }

    @Test
    @DisplayName("Should block user if they already have an entry today and have no active session")
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
    @DisplayName("Should allow conversation if the user already has an active session (even with an entry in the database)")
    void shouldAllowIfSessionIsActive() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getCurrentHistorySize(userId)).thenReturn(0);
        when(chatService.processMessage(any(), any())).thenReturn(Flux.just("AI Response"));

        Flux<String> response = journalingService.handleUserMessage(userId, "Continuing...");

        StepVerifier.create(response)
                .expectNext("AI Response")
                .verifyComplete();

        verify(sessionManager).keepAlive(userId);
    }

    @Test
    @DisplayName("Should allow conversation if no entry exists in the database")
    void shouldAllowUserIfJournalDoesntExistToday() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(false);
        when(ingestionService.hasJournaledToday(userId)).thenReturn(false);

        when(chatService.processMessage(eq(userId), anyString()))
                .thenReturn(Flux.just("AI Response"));

        Flux<String> response = journalingService.handleUserMessage(userId, "Hi");

        StepVerifier.create(response)
                .expectNext("AI Response")
                .verifyComplete();

        verify(sessionManager).keepAlive(userId);
    }

    @Test
    @DisplayName("Should automatically consolidate when the message limit is reached")
    void shouldAutoConsolidateWhenLimitReached() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);

        int limitInMessages = MAX_INTERACTIONS_BEFORE_SAVE * 2;
        when(chatService.getCurrentHistorySize(userId)).thenReturn(limitInMessages);

        when(chatService.getHistorySnapshot(userId)).thenReturn("Full History");

        Flux<String> response = journalingService.handleUserMessage(userId, "One more message");

        StepVerifier.create(response)
                .expectNext(ReplyMessages.CONSOLIDATE)
                .verifyComplete();

        verify(ingestionService).ingest(anyString(), eq(userId));
        verify(chatService).clearHistory(userId);
        verify(sessionManager).invalidate(userId);
        verify(sseNotificationService).removeEmitter(userId);
    }

    @Test
    @DisplayName("Should not automatically consolidate when below the message limit")
    void shouldNotAutoConsolidateWhenLimitNotReached() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getCurrentHistorySize(userId)).thenReturn(1);

        // NPE FIX: Teaching the Mock to respond for the normal flow
        when(chatService.processMessage(eq(userId), anyString()))
                .thenReturn(Flux.just("AI Response"));

        Flux<String> response = journalingService.handleUserMessage(userId, "One more message");

        StepVerifier.create(response)
                .expectNext("AI Response")
                .verifyComplete();

        verify(ingestionService, never()).ingest(anyString(), eq(userId));
        verify(chatService, never()).clearHistory(userId);
        verify(chatService).processMessage(eq(userId), anyString());
    }

    @Test
    @DisplayName("Should manually consolidate and close when requested by the user")
    void shouldManuallyConsolidateWhenRequested() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(true);
        when(chatService.getHistorySnapshot(userId)).thenReturn("History to Save");

        Flux<String> response = journalingService.manuallyCloseConversation(userId);

        StepVerifier.create(response)
                .expectNext(ReplyMessages.MANUALLY_CONSOLIDATE)
                .verifyComplete();

        verify(ingestionService).ingest("History to Save", userId);
        verify(chatService).clearHistory(userId);
        verify(sessionManager).invalidate(userId);
        verify(sseNotificationService).removeEmitter(userId);
    }

    @Test
    @DisplayName("Should throw an error when trying to manually close without an active session")
    void shouldThrowErrorWhenManualClosingWithoutSession() {
        when(sessionManager.hasActiveSession(userId)).thenReturn(false);

        // Verifies Fail Fast
        assertThrows(IllegalStateException.class, () ->
                journalingService.manuallyCloseConversation(userId)
        );

        verify(ingestionService, never()).ingest(anyString(), any());
        verify(chatService, never()).clearHistory(any());
        verify(sessionManager, never()).invalidate(any());
    }

    @Test
    @DisplayName("Should register callbacks in SessionManager on initialization")
    void shouldRegisterCallbacksOnInit() {
        verify(sessionManager).setCallbacks(any(), any());
    }
}