package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.DailyInfo;
import com.example.life_tracker.api.domain.domain.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
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
    private final VectorStore vectorStore;

    public DailyInfo handleUserMessage(String userMessage) {

        return extractMessageInfo(userMessage);
    }

    private DailyInfo extractMessageInfo(String message) {

        var outputConverter = new BeanOutputConverter<>(DailyInfo.class);

        String prompt = PromptTemplates.INFO_EXTRACTION.formatted(LocalDate.now(), message);

        DailyInfo dailyInfo = chatClient.prompt()
                .user(u -> u.text(prompt)
                .param("format_instructions", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);

        return dailyInfo;
    };

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
