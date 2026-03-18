package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.model.DailyInfo;
import com.example.life_tracker.api.domain.domain.mapper.DailyInfoMapper;
import com.example.life_tracker.api.domain.domain.PromptTemplates;
import com.example.life_tracker.api.domain.domain.validator.DailyInfoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JournalingIngestionService {

    private final ChatClient chatClient;
    private final DailyInfoMapper dailyInfoMapper;
    private final VectorStore vectorStore;
    private final DailyInfoValidator dailyInfoValidator;

    @Async
    public void ingest(String chatHistorySnapshot, UUID userId) {
        log.info("Starting background consolidation for user {}", userId);

        try {
            DailyInfo dailyInfo = extractMessageInfo(chatHistorySnapshot);
            dailyInfoValidator.validate(dailyInfo);
            storeDailyInfo(dailyInfo, userId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error consolidating entry for user {}", userId, e);
        }
    }

    public boolean hasJournaledToday(UUID userId) {
        String todayDate = LocalDate.now().toString();


        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        Expression expression = filter.and(
                filter.eq("userId", userId.toString()),
                filter.eq("date", todayDate)
        ).build();

        List<Document> result = vectorStore.similaritySearch(
                SearchRequest.builder().query("check").filterExpression(expression).topK(1).build()
        );

        return !result.isEmpty();
    }

    private DailyInfo extractMessageInfo(String history) {
        var outputConverter = new BeanOutputConverter<>(DailyInfo.class);

        String prompt = PromptTemplates.INFO_EXTRACTION.formatted(history, LocalDate.now());

        return chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("format_instructions", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);
    }

    private void storeDailyInfo(DailyInfo dailyInfo, UUID userId) {
        List<Document> documents = dailyInfo.items().stream()
                .map(item -> dailyInfoMapper.toDocument(item, userId))
                .filter(Objects::nonNull)
                .toList();

        vectorStore.add(documents);
    }

}
