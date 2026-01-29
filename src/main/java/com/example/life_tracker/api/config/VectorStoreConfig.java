package com.example.life_tracker.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {
    @Bean
    public GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails(
            @Value("${spring.ai.google.genai.api-key}") String apiKey) {

        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.google.genai.embedding.options.model:text-embedding-004}") String modelName
            ) {
        var connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)
                .build();

        var options = GoogleGenAiTextEmbeddingOptions.builder()
                .model(modelName)
                .build();

        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

}
