package com.example.life_tracker.IT;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.List;
import java.util.concurrent.Executor;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return new FakeEmbeddingModel();
    }
}
