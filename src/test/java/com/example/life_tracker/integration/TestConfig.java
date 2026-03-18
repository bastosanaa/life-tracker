package com.example.life_tracker.integration;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return new FakeEmbeddingModel();
    }
}
