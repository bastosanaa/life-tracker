package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Value("${app.mock-ai:false}")
    private boolean mockAiEnabled;

    private static final String CONVERSATION_ID_PREFIX = "chat-";

    public Flux<String> processMessage(UUID userId, String userMessage) {
        String conversationId = getConversationId(userId);

        saveUserMessage(conversationId, userMessage);

        String prompt = getPrompt(userId);

        return streamMessageReply(prompt, conversationId);
    }

    public String getHistorySnapshot(UUID userId) {
        return getHistoryAsText(userId);
    }

    public void clearHistory(UUID userId) {
        String conversationId = getConversationId(userId);
        chatMemory.clear(conversationId);
    }

    public int getCurrentHistorySize(UUID userId) {
        String conversationId = getConversationId(userId);
        log.info("Current history size {}", chatMemory.get(conversationId).size());
        return chatMemory.get(conversationId).size();
    }

    private String getConversationId(UUID userId) {
        return CONVERSATION_ID_PREFIX + userId.toString();
    }

    private void saveUserMessage(String conversationId, String message) {
        chatMemory.add(conversationId, new UserMessage(message));
    }

    private String getPrompt(UUID userId) {
        String history = getHistoryAsText(userId);
        return PromptTemplates.CONVERSATION_MODE.formatted(history, userId);
    }

    private String getHistoryAsText(UUID userId) {
        String conversationId = getConversationId(userId);
        List<Message> history = chatMemory.get(conversationId);

        return history.stream()
                .map(m -> {
                    String role = m.getMessageType() == MessageType.USER ? "Usuário" : "IA";
                    return role + ": " + m.getText();
                })
                .collect(Collectors.joining("\n"));
    }

    private Flux<String> streamMessageReply(String prompt, String conversationId) {
        StringBuffer contentBuffer = new StringBuffer();

            //MOCKING AI
//            return Flux.just("Isso ", "é ", "uma ", "resposta ", "simulada ", "do ", "modo ", "de ", "teste.")
//                    .delayElements(Duration.ofMillis(100)) // Simula delay da rede
//                    .doOnComplete(() -> saveAssistantMessage(conversationId, "Isso é uma resposta simulada do modo de teste."));

        return chatClient.prompt()
                .user(u -> u.text(prompt))
                .stream()
                .content()
                .doOnNext(contentBuffer::append)
                .doOnComplete(() -> {
                    String fullResponse = contentBuffer.toString();
                    saveAssistantMessage(conversationId, fullResponse);
                })
                .doOnError(e -> log.error("Error generating response", e));
    }

    private void saveAssistantMessage(String conversationId, String message) {
        chatMemory.add(conversationId, new AssistantMessage(message));
    }
}
