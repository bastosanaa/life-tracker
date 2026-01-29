package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalingService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final JournalingIngestionService consolidationService;

    private static final int MAX_INTERACTIONS_BEFORE_SAVE = 1;

    private static final String CONVERSATION_ID = "user-default"; //TODO: adicionar id usuário logado
    private static final UUID DEFAULT_USER_ID = UUID.fromString("9cfd9fa2-110e-49a3-8148-65daa18d9c68");

    public String handleUserMessage(String userMessage) {
        String chatOutput;

        chatMemory.add(CONVERSATION_ID, new UserMessage(userMessage));

        String history = getHistoryAsText();

        int currentHistorySize = chatMemory.get(CONVERSATION_ID).size();
        boolean shouldConsolidate = (currentHistorySize / 2) >= MAX_INTERACTIONS_BEFORE_SAVE;

        if (shouldConsolidate) {
            consolidationService.ingest(history, DEFAULT_USER_ID);
            chatOutput = "Entendido! Guardei essas informações no seu diário. Até amanhã!"; // TODO: melhorar a resposta final do chat levando em conta o contexto da conversa
//            chatMemory.clear(CONVERSATION_ID);
        } else {
            chatOutput = generateNextChatInteraction(userMessage, history);
        }
      return chatOutput;
    }

    private String generateNextChatInteraction(String userMessage, String history) {

        String prompt = PromptTemplates.CONVERSATION_MODE.formatted(history, userMessage);
        String chatMessage = chatClient.prompt()
                .user(u -> u.text(prompt))
                .call()
                .content();

        assert chatMessage != null;
        chatMemory.add(CONVERSATION_ID, new AssistantMessage(chatMessage));

        return chatMessage;
    }

    private String getHistoryAsText() {
        List<Message> history = chatMemory.get(CONVERSATION_ID);

        return history.stream()
                .map(m -> {
                    String role = m.getMessageType() == MessageType.USER ? "Usuário" : "IA";
                    return role + ": " + m.getText();
                })
                .collect(Collectors.joining("\n"));
    }

}
