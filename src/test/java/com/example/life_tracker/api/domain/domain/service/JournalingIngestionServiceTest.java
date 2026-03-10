package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.mapper.DailyInfoMapper;
import com.example.life_tracker.api.domain.domain.model.DailyInfo;
import com.example.life_tracker.api.domain.domain.validator.DailyInfoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalingIngestionServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private DailyInfoMapper dailyInfoMapper;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DailyInfoValidator dailyInfoValidator;

    @InjectMocks
    private JournalingIngestionService ingestionService;

    private final UUID userId = UUID.randomUUID();
    private DailyInfo mockDailyInfo;
    private Document mockDocument;

    @Captor
    private ArgumentCaptor<List<Document>> documentListCaptor;

    @BeforeEach
    void setup() {
        DailyInfo.InfoItem item = new DailyInfo.InfoItem(
                "Estudou Spring Boot",
                DailyInfo.Category.STUDIES,
                DailyInfo.Feeling.GOOD,
                "2026-03-09",
                false,
                null
        );
        mockDailyInfo = new DailyInfo(List.of(item));

        mockDocument = new Document("Estudou Spring Boot", Map.of("userId", userId));
    }

    @Test
    @DisplayName("Should extract, validate, map and save the daily info in the VectorStore successfully")
    void shouldIngestSuccessfully() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        when(callResponseSpec.entity(any(BeanOutputConverter.class))).thenReturn(mockDailyInfo);

        when(dailyInfoMapper.toDocument(any(DailyInfo.InfoItem.class), eq(userId))).thenReturn(mockDocument);

        ingestionService.ingest("Conversation history", userId);

        verify(dailyInfoValidator).validate(mockDailyInfo);

        verify(vectorStore).add(documentListCaptor.capture());

        List<Document> savedDocuments = documentListCaptor.getValue();
        assertEquals(1, savedDocuments.size());
        assertEquals("Estudou Spring Boot", savedDocuments.getFirst().getText());
    }

    @Test
    @DisplayName("Should silently abort the process (catch exception) if AI fails during extraction")
    void shouldCatchExceptionAndNotSaveWhenExtractionFails() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(any(BeanOutputConverter.class)))
                .thenThrow(new RuntimeException("AI parse error"));

        ingestionService.ingest("History with issue", userId);

        verify(dailyInfoValidator, never()).validate(any());
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Should abort the process if data returned by the AI is rejected by the Validator")
    void shouldCatchExceptionAndNotSaveWhenValidationFails() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(any(BeanOutputConverter.class))).thenReturn(mockDailyInfo);

        doThrow(new IllegalArgumentException("Item without a defined date."))
                .when(dailyInfoValidator).validate(mockDailyInfo);

        ingestionService.ingest("History with issue", userId);

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Should return TRUE when documents are found for the user on today's date")
    void shouldReturnTrueWhenHasJournaledToday() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(mockDocument));

        boolean result = ingestionService.hasJournaledToday(userId);

        assertTrue(result, "Should return true because a journal entry was found for today");
    }

    @Test
    @DisplayName("Should return FALSE when no documents are found in the database for the user today")
    void shouldReturnFalseWhenHasNotJournaledToday() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        boolean result = ingestionService.hasJournaledToday(userId);

        assertFalse(result, "Should return false because no journal entry was found for today");
    }
}