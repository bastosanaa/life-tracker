package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.ReplyMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseNotificationService {

    private static final String EVENT_NAME_WARNING = "WARNING";
    private final Map<UUID, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        setCleaningCallbacks(emitter, userId);

        activeEmitters.put(userId, emitter);
        log.info("New SSE connection registered for user: {}", userId);

        return emitter;
    };

    public void removeEmitter(UUID userId) {
        SseEmitter emitter = activeEmitters.remove(userId);
        boolean isUserConnected = emitter != null;

        if (isUserConnected) {
            emitter.complete();
            log.info("SSE connection manually closed for user: {}", userId);
        }
    }

    public void sendInactivityWarning(UUID userId) {
        SseEmitter emitter = activeEmitters.get(userId);

        if (!isUserConnected(emitter)) {
            log.debug("Warning cancelled: user disconnected. ID: {}", userId);
            return;
        }

        sendWarning(userId, emitter);
    }

    private boolean isUserConnected(SseEmitter emitter) {
        return emitter != null;
    }

    private void sendWarning(UUID userId, SseEmitter emitter) {
        try {
            emitWarningEvent(userId, emitter);
        } catch (Exception e) {
            log.warn("Failed to send SSE warning. Removing user: {}", userId);
            disconnectUser(userId);
        }
    }

    private void emitWarningEvent(UUID userId, SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event()
                .name(EVENT_NAME_WARNING)
                .data(ReplyMessages.INACTIVITY_WARNING));
        log.info("Inactivity warning sent successfully to user: {}", userId);
    }

    private void setCleaningCallbacks(SseEmitter emitter, UUID userId) {
        emitter.onCompletion(() -> {
            log.debug("SSE completed for user: {}", userId);
            disconnectUser(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user: {}", userId);
            emitter.complete();
            disconnectUser(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE connection error for user {}: {}", userId, e.getMessage());
            emitter.completeWithError(e);
            disconnectUser(userId);
        });
    }

    private void disconnectUser(UUID userId) {
        activeEmitters.remove(userId);
    }
}


