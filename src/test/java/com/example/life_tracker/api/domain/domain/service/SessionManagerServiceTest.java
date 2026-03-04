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
    @DisplayName("Deve retornar false quando não houver sessão ativa para o usuário")
    void shouldReturnFalseWhenNoSession() {
        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertFalse(hasSession);
    }

    @Test
    @DisplayName("Deve criar uma sessão e mantê-la ativa ao chamar keepAlive")
    void shouldCreateSessionOnKeepAlive() {
        sessionManager.keepAlive(userId);

        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertTrue(hasSession);
    }

    @Test
    @DisplayName("Deve retornar true quando houver sessão ativa para o usuário")
    void shouldReturnTrueWhenSession() {
        sessionManager.keepAlive(userId);
        boolean hasSession = sessionManager.hasActiveSession(userId);
        assertTrue(hasSession);
    }

    @Test
    @DisplayName("Deve remover a sessão instantaneamente ao chamar invalidate")
    void shouldRemoveSessionOnInvalidate() {
        sessionManager.keepAlive(userId);
        assertTrue(sessionManager.hasActiveSession(userId));

        sessionManager.invalidate(userId);

        assertFalse(sessionManager.hasActiveSession(userId));
    }

    @Test
    @DisplayName("Ciclo 1: Deve acionar onWarning e mudar estado para WARNED na primeira expiração")
    void shouldTriggerWarningOnFirstExpiration() {
        sessionManager.keepAlive(userId);

        ReflectionTestUtils.invokeMethod(sessionManager, "handleExpiration", userId, SessionState.ACTIVE);

        verify(onWarningCallback).accept(userId);
        verify(onExpirationCallback, never()).accept(any());

        assertTrue(sessionManager.hasActiveSession(userId));
    }

    @Test
    @DisplayName("Ciclo 2: Deve acionar onExpiration e invalidar sessão na segunda expiração (WARNED)")
    void shouldTriggerExpirationAndRemoveOnSecondExpiration() {
        sessionManager.keepAlive(userId);

        ReflectionTestUtils.invokeMethod(sessionManager, "handleExpiration", userId, SessionState.WARNED);

        verify(onExpirationCallback).accept(userId);
        verify(onWarningCallback, never()).accept(any());

        assertFalse(sessionManager.hasActiveSession(userId));
    }
}