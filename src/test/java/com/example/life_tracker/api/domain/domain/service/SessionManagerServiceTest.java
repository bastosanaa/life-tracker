package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.model.SessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionManagerServiceTest {

    @Mock private Consumer<UUID> onWarningCallback;
    @Mock private Consumer<UUID> onExpirationCallback;

    @InjectMocks
    private SessionManagerService sessionManager;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        // Initialize cache Caffeine
        sessionManager.init();
        sessionManager.setCallbacks(onWarningCallback, onExpirationCallback);
    }

    @Test
    @DisplayName("Should return false when there is no active session for the user")
    void shouldReturnFalseWhenNoSession() {
        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertFalse(hasSession);
    }

    @Test
    @DisplayName("Should create a session and keep it active when calling keepAlive")
    void shouldCreateSessionOnKeepAlive() {
        sessionManager.keepAlive(userId);

        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertTrue(hasSession);
    }

    @Test
    @DisplayName("Should return true when there is an active session for the user")
    void shouldReturnTrueWhenSession() {
        sessionManager.keepAlive(userId);
        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertTrue(hasSession);
    }

    @Test
    @DisplayName("Should immediately remove the session when calling invalidate")
    void shouldRemoveSessionOnInvalidate() {
        sessionManager.keepAlive(userId);
        assertTrue(sessionManager.hasActiveSession(userId));

        sessionManager.invalidate(userId);

        assertFalse(sessionManager.hasActiveSession(userId));
    }

    @Test
    @DisplayName("Cycle 1: Should trigger onWarning and change state to WARNED on first expiration")
    void shouldTriggerWarningOnFirstExpiration() {
        sessionManager.keepAlive(userId);

        ReflectionTestUtils.invokeMethod(sessionManager, "handleExpiration", userId, SessionState.ACTIVE);

        verify(onWarningCallback).accept(userId);
        verify(onExpirationCallback, never()).accept(any());

        assertTrue(sessionManager.hasActiveSession(userId));
    }

    @Test
    @DisplayName("Cycle 2: Should trigger onExpiration and invalidate session on second expiration (WARNED)")
    void shouldTriggerExpirationAndRemoveOnSecondExpiration() {
        sessionManager.keepAlive(userId);

        ReflectionTestUtils.invokeMethod(sessionManager, "handleExpiration", userId, SessionState.WARNED);

        verify(onExpirationCallback).accept(userId);
        verify(onWarningCallback, never()).accept(any());

        assertFalse(sessionManager.hasActiveSession(userId));
    }
}