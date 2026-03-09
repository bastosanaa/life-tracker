package com.example.life_tracker.api.domain.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class ChatServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock private ChatMemory chatMemory;

    @InjectMocks
    private ChatService chatService;

    private final UUID userId = UUID.randomUUID();
    private final String conversationId = "chat-" + userId.toString();

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Test
    @DisplayName("Deve processar a mensagem do utilizador chamando a API do Spring AI e guardando no histórico")
    void shouldProcessMessageAndReturnFlux() {
        when(chatMemory.get(conversationId)).thenReturn(List.of());

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .stream()
                .content()
        ).thenReturn(Flux.just("A ", "IA ", "respondeu ", "com ", "sucesso!"));

        Flux<String> response = chatService.processMessage(userId, "Olá, o meu dia foi bom!");

        StepVerifier.create(response)
                .expectNext("A ", "IA ", "respondeu ", "com ", "sucesso!")
                .verifyComplete();

        verify(chatMemory, times(2)).add(eq(conversationId), messageCaptor.capture());

        List<Message> capturedMessages = messageCaptor.getAllValues();

        assertEquals("Olá, o meu dia foi bom!", capturedMessages.get(0).getText());
        assertEquals("USER", capturedMessages.get(0).getMessageType().name());

        assertEquals("A IA respondeu com sucesso!", capturedMessages.get(1).getText());
        assertEquals("ASSISTANT", capturedMessages.get(1).getMessageType().name());
    }

    @Test
    @DisplayName("Deve devolver o snapshot do histórico formatado em texto para a IA")
    void shouldReturnFormattedHistorySnapshot() {
        List<Message> mockHistory = List.of(
                new UserMessage("Fui correr de manhã"),
                new AssistantMessage("Que ótimo! E como se sentiu?")
        );
        when(chatMemory.get(conversationId)).thenReturn(mockHistory);

        String snapshot = chatService.getHistorySnapshot(userId);

        String expected = "Usuário: Fui correr de manhã\nIA: Que ótimo! E como se sentiu?";
        assertEquals(expected, snapshot);
    }

    @Test
    @DisplayName("Deve limpar o histórico da memória corretamente")
    void shouldClearHistory() {
        chatService.clearHistory(userId);

        verify(chatMemory).clear(conversationId);
    }

    @Test
    @DisplayName("Deve devolver o tamanho atual do histórico")
    void shouldReturnCurrentHistorySize() {
        List<Message> mockHistory = List.of(
                new UserMessage("msg1"),
                new AssistantMessage("msg2"),
                new UserMessage("msg3")
        );
        when(chatMemory.get(conversationId)).thenReturn(mockHistory);

        int size = chatService.getCurrentHistorySize(userId);

        assertEquals(3, size);
    }

}
