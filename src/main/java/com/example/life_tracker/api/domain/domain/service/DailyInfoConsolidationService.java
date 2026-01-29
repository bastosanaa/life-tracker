package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.DailyInfo;
import com.example.life_tracker.api.domain.domain.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyInfoConsolidationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Async
    public void consolidateAsync(String chatHistorySnapshot) {
        System.out.println("Iniciando consolidação em background...");

        DailyInfo dailyInfo = extractMessageInfo(chatHistorySnapshot);
        System.out.println(dailyInfo);

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

}
