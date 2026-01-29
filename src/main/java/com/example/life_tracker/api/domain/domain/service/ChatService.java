package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.DailyInfo;
import com.example.life_tracker.api.domain.domain.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    private static final int MAX_INTERACTIONS_BEFORE_SAVE = 3;
    private static final String CONVERSATION_ID = "user-default"; //TODO: adicionar id usuário logado

    public String handleUserMessage(String userMessage) {
        String chatOutput;

        chatMemory.add(CONVERSATION_ID, new UserMessage(userMessage));

        int currentHistorySize = chatMemory.get(CONVERSATION_ID).size();
        boolean shouldConsolidate = (currentHistorySize / 2) >= MAX_INTERACTIONS_BEFORE_SAVE;

        if (shouldConsolidate) {
            consolidateDailyInput();
            chatOutput = "Entendido! Guardei essas informações no seu diário. Até amanhã!";
        } else {
            chatOutput = generateNextChatInteraction(userMessage);
        }
      return chatOutput;
    }

    private String generateNextChatInteraction(String userMessage) {
        String history = getHistoryAsText();

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

    private void consolidateDailyInput() {
        String history = getHistoryAsText();
        DailyInfo dailyInfo = extractMessageInfo(history);
        System.out.println("DailyInfo: " + dailyInfo);

        //TODO: salvar no banco
    }

    private DailyInfo extractMessageInfo( String history) {

        var outputConverter = new BeanOutputConverter<>(DailyInfo.class);

        String prompt = PromptTemplates.INFO_EXTRACTION.formatted(history, LocalDate.now());

        DailyInfo dailyInfo = chatClient.prompt()
                .user(u -> u.text(prompt)
                .param("format_instructions", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);

        System.out.println(dailyInfo);
        return dailyInfo;
    };
}
