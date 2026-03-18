package com.example.life_tracker.integration;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.util.List;
import java.util.Random;

public class FakeEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 768;
    private final Random random = new Random(42); // seed fixo para reprodutibilidade

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = request.getInstructions().stream()
                .map(text -> new Embedding(randomVector(), 0))
                .toList();
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return randomVector();
    }

    private float[] randomVector() {
        float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }
}
