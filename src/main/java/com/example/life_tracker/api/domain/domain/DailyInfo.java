package com.example.life_tracker.api.domain.domain;

import java.util.List;

public record DailyInfo(
        List<InfoItem> items,
        List<String> pendingQuestions
) {
    public record InfoItem(
            String summary,
            Category category,
            Feeling feeling,
            String date,
            boolean futureScheduling,
            String futureMessage
    ) {}

    public enum Category {WORK, STUDIES, HEALTH, ENTERTAINMENT, RELATIONSHIPS, OTHER}
    public enum Feeling {GOOD, BAD, NEUTRAL}
}
