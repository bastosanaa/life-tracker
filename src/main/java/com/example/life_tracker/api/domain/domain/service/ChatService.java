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

    private static final String CONVERSATION_ID = "user-default"; //TODO: adicionar id usuário logado

    public String handleUserMessage(String userMessage) {

        String history = getChatHistory(userMessage);
        DailyInfo extractedInfo = extractMessageInfo(userMessage, history);

        String responseText;

        if (extractedInfo.pendingQuestions() != null && !extractedInfo.pendingQuestions().isEmpty()) {
            // Cenario A: Informação incompleta. A IA devolve a pergunta gerada.
            responseText = extractedInfo.pendingQuestions().get(0);
        } else {
            // Cenario B: Informação completa. Confirmação.
            // Poderíamos chamar o LLM aqui para gerar um agradecimento mais natural se quisesse.
            responseText = "Entendido! Guardei essas informações no seu diário. Mais alguma coisa?";
        }

        chatMemory.add(CONVERSATION_ID, new UserMessage(userMessage));
        chatMemory.add(CONVERSATION_ID, new AssistantMessage(responseText));

        return responseText;
    }

    private DailyInfo extractMessageInfo(String message, String history) {

        var outputConverter = new BeanOutputConverter<>(DailyInfo.class);

        String prompt = PromptTemplates.INFO_EXTRACTION.formatted(history,LocalDate.now(), message);

        DailyInfo dailyInfo = chatClient.prompt()
                .user(u -> u.text(prompt)
                .param("format_instructions", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);

        System.out.println(dailyInfo);
        return dailyInfo;
    };

    private String getChatHistory(String userMessageText) {
        List<Message> history = chatMemory.get(CONVERSATION_ID);

        return history.stream()
                .map(m -> (m.getMessageType() == MessageType.USER ? "Usuário: " : "IA: ") + m.getText())
                .collect(Collectors.joining("\n"));

    }

//    private List<Document> retrieveSimilarDocuments(String searchRequest) {
//        return  vectorStore.similaritySearch(
//                SearchRequest.builder()
//                        .query(searchRequest)
//                        .topK(3)
//                        .build()
//        );
//    }

//    private String augmentContext(List<Document> documents) {
//        return documents.stream()
//                .map(Document::getText)
//                .collect(Collectors.joining("\n\n"));
//    }
//
//    private String generateAnswer(String userPrompt , String context) {
//
//        String systemMessage = PromptTemplates.CHAT.formatted(context);
//
//        return chatClient.prompt()
//                .system(systemMessage)
//                .user(userPrompt)
//                .call()
//                .content();
//    }
}
