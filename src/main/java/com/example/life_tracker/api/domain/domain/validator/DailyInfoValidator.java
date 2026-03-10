package com.example.life_tracker.api.domain.domain.validator;

import com.example.life_tracker.api.domain.domain.model.DailyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class DailyInfoValidator {
    public void validate(DailyInfo dailyInfo) {
        if (dailyInfo == null || dailyInfo.items() == null || dailyInfo.items().isEmpty()) {
            throw new IllegalArgumentException("AI returned empty or null data.");
        }

        dailyInfo.items().forEach(this::validateCriticalFields);
    }

    private void validateCriticalFields(DailyInfo.InfoItem item) {
        if (item.summary() == null || item.summary().trim().isEmpty()) {
            throw new IllegalArgumentException("Item with missing summary detected.");
        }

        if (item.date() == null) {
            throw new IllegalArgumentException("Item without a defined date.");
        }
        try {
            LocalDate.parse(item.date());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date received from AI: " + item.date());
        }
    }
}
