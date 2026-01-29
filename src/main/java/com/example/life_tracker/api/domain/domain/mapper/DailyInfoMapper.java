package com.example.life_tracker.api.domain.domain.mapper;

import com.example.life_tracker.api.domain.domain.model.DailyInfo;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DailyInfoMapper {
    private static final String META_USER_ID = "userId";
    private static final String META_CATEGORY = "category";
    private static final String META_FEELING = "feeling";
    private static final String META_DATE = "date";
    private static final String META_FUTURE_SCHEDULING = "futureScheduling";

    public Document toDocument(DailyInfo.InfoItem item, UUID userId) {
        if (item == null) return null;

        String categoryName = item.category() != null ? item.category().name() : "OTHER";
        String feelingName = item.feeling() != null ? item.feeling().name() : "NEUTRAL";
        String dateVal = item.date() != null ? item.date() : "Unknown";

        Map<String, Object> metadata = Map.of(
                META_USER_ID, userId,
                META_CATEGORY, categoryName,
                META_FEELING, feelingName,
                META_DATE, dateVal,
                META_FUTURE_SCHEDULING, item.futureScheduling()
        );

        return new Document(item.summary(), metadata);
    }
}
