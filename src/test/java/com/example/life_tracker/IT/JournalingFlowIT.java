package com.example.life_tracker.IT;
import com.example.life_tracker.api.domain.domain.model.DailyInfo;
import com.example.life_tracker.api.domain.domain.service.ChatService;
import com.example.life_tracker.api.domain.domain.service.JournalingIngestionService;
import com.example.life_tracker.api.domain.domain.service.JournalingService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
public class JournalingFlowIT extends IntegrationTestBase {
    @Autowired
    private JournalingService journalingService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private JournalingIngestionService ingestionService;

    @Autowired
    private VectorStore vectorStore;

    @MockitoBean
    private ChatClient chatClient;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        chatService.clearHistory(userId);
    }

    @Test
    @DisplayName("Deve processar mensagens, acumular histórico e vetorizar ao consolidar")
    void shouldProcessMessagesAndVectorizeOnConsolidation() {
        var callSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var streamSpec = mock(ChatClient.StreamResponseSpec.class);
        var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        var converter = new BeanOutputConverter<>(DailyInfo.class);
        DailyInfo dailyInfo = converter.convert(buildFakeDailyInfoJson());

        when(chatClient.prompt()).thenReturn(callSpec);
        when(callSpec.user(any(Consumer.class))).thenReturn(callSpec);

        when(callSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("Que legal! Me conta mais."));

        when(callSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(any(BeanOutputConverter.class))).thenReturn(dailyInfo);

        journalingService.handleUserMessage(userId, "Hoje estudei bastante coisa nova.").blockLast();
        journalingService.handleUserMessage(userId, "Estudei Testcontainers com Spring Boot.").blockLast();
        journalingService.handleUserMessage(userId, "Também fiz uma caminhada de 5km.").blockLast();
        journalingService.handleUserMessage(userId, "Só isso por hoje.").blockLast();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(ingestionService.hasJournaledToday(userId)).isTrue()
                );
    }

    @Test
    @DisplayName("Deve armazenar os itens com metadados corretos no VectorStore")
    void shouldStoreItemsWithCorrectMetadata() throws InterruptedException {
        String history = buildFakeHistory();
        mockChatClientExtractionReply(buildFakeDailyInfoJson());

        ingestionService.ingest(history, userId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Document> results = searchByUserAndDate(userId, LocalDate.now().toString());

                    assertThat(results).isNotEmpty();

                    Document studiesDoc = results.stream()
                            .filter(d -> "STUDIES".equals(d.getMetadata().get("category")))
                            .findFirst()
                            .orElseThrow();

                    assertThat(studiesDoc.getMetadata()).containsEntry("feeling", "GOOD");
                    assertThat(studiesDoc.getMetadata()).containsEntry("futureScheduling", false);
                    assertThat(studiesDoc.getText()).contains("Testcontainers");
                });
    }

    @Test
    @DisplayName("Deve bloquear segunda entrada no mesmo dia após consolidação")
    void shouldBlockSecondJournalingOnSameDay() {
        mockChatClientExtractionReply(buildFakeDailyInfoJson());
        ingestionService.ingest(buildFakeHistory(), userId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> ingestionService.hasJournaledToday(userId));

        List<String> responses = journalingService
                .handleUserMessage(userId, "Quero adicionar mais coisas.")
                .collectList()
                .block();

        assertThat(responses).containsExactly("Você já registrou seu dia hoje! Volte amanhã para contar mais novidades.");
    }

    @Test
    @DisplayName("Deve limpar o histórico do chat após consolidação")
    void shouldClearChatHistoryAfterConsolidation() {
        mockChatClientConversationalReply("Entendi! Me conta mais.");
        journalingService.handleUserMessage(userId, "Trabalhei no projeto hoje.").blockLast();

        assertThat(chatService.getCurrentHistorySize(userId)).isGreaterThan(0);

        mockChatClientExtractionReply(buildFakeDailyInfoJson());
        journalingService.manuallyCloseConversation(userId).blockLast();

        assertThat(chatService.getCurrentHistorySize(userId)).isZero();
    }

    private void mockChatClientConversationalReply(String reply) {
        var callSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt()).thenReturn(callSpec);
        when(callSpec.user(any(Consumer.class))).thenReturn(callSpec);
        when(callSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(reactor.core.publisher.Flux.just(reply));
    }

    private void mockChatClientExtractionReply(String jsonReply) {
        var callSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        var converter = new BeanOutputConverter<>(DailyInfo.class);
        DailyInfo dailyInfo = converter.convert(jsonReply);

        when(chatClient.prompt()).thenReturn(callSpec);
        when(callSpec.user(any(Consumer.class))).thenReturn(callSpec);
        when(callSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(any(BeanOutputConverter.class))).thenReturn(dailyInfo);
    }

    private List<Document> searchByUserAndDate(UUID userId, String date) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        Filter.Expression expression = filter.and(
                filter.eq("userId", userId.toString()),
                filter.eq("date", date)
        ).build();

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("estudos saúde trabalho")
                        .filterExpression(expression)
                        .topK(10)
                        .build()
        );
    }

    private String buildFakeHistory() {
        return """
            Usuário: Hoje estudei Testcontainers com Spring Boot.
            IA: Que legal! Como foi a experiência?
            Usuário: Foi muito boa, aprendi bastante sobre testes de integração.
            """;
    }

    private String buildFakeDailyInfoJson() {
        return """
            {
              "items": [
                {
                  "summary": "Estudou Testcontainers com Spring Boot e aprendeu sobre testes de integração.",
                  "category": "STUDIES",
                  "feeling": "GOOD",
                  "date": "%s",
                  "futureScheduling": false,
                  "futureMessage": null
                }
              ]
            }
            """.formatted(LocalDate.now());
    }
}
