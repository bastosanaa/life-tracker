package com.example.life_tracker.api.domain.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SseNotificationServiceTest {

    private SseNotificationService sseNotificationService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        sseNotificationService = new SseNotificationService();
    }

    @Test
    @DisplayName("Should add a new SseEmitter and register it in the active users map")
    void shouldAddEmitter() {
        SseEmitter emitter = sseNotificationService.addEmitter(userId);

        assertNotNull(emitter, "The returned SseEmitter must not be null");
        assertEquals(Long.MAX_VALUE, emitter.getTimeout(), "Timeout must be set to Long.MAX_VALUE");

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");

        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId), "The map must contain the user ID");
        assertEquals(emitter, activeEmitters.get(userId), "The stored emitter must be the same one that was returned");
    }

    @Test
    @DisplayName("Should remove the SseEmitter from the map and complete it")
    void shouldRemoveEmitter() {
        sseNotificationService.addEmitter(userId);

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");
        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId));

        sseNotificationService.removeEmitter(userId);

        assertFalse(activeEmitters.containsKey(userId), "The user ID must be removed from the map");
    }

    @Test
    @DisplayName("Should not throw an error when sending a warning to a disconnected user")
    void shouldNotThrowErrorWhenSendingWarningToDisconnectedUser() {
        assertDoesNotThrow(() -> sseNotificationService.sendInactivityWarning(userId));
    }

    @Test
    @DisplayName("Should successfully send a warning to a connected user")
    void shouldSendWarningSuccessfully() {
        sseNotificationService.addEmitter(userId);

        assertDoesNotThrow(() -> sseNotificationService.sendInactivityWarning(userId));

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");
        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId), "The user must remain in the map after a successful send");
    }
}
