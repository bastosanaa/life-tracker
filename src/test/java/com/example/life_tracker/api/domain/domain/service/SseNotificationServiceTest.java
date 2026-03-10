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
    @DisplayName("Deve adicionar um novo SseEmitter e registá-lo no mapa de utilizadores ativos")
    void shouldAddEmitter() {
        SseEmitter emitter = sseNotificationService.addEmitter(userId);

        assertNotNull(emitter, "O SseEmitter retornado não deve ser nulo");
        assertEquals(Long.MAX_VALUE, emitter.getTimeout(), "O timeout deve ser configurado para Long.MAX_VALUE");

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");

        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId), "O mapa deve conter o ID do utilizador");
        assertEquals(emitter, activeEmitters.get(userId), "O emitter guardado deve ser o mesmo que foi retornado");
    }

    @Test
    @DisplayName("Deve remover o SseEmitter do mapa e completá-lo")
    void shouldRemoveEmitter() {
        sseNotificationService.addEmitter(userId);

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");
        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId));

        sseNotificationService.removeEmitter(userId);

        assertFalse(activeEmitters.containsKey(userId), "O ID do utilizador deve ser removido do mapa");
    }

    @Test
    @DisplayName("Não deve lançar erro ao tentar enviar aviso para um utilizador desconectado")
    void shouldNotThrowErrorWhenSendingWarningToDisconnectedUser() {
        assertDoesNotThrow(() -> sseNotificationService.sendInactivityWarning(userId));
    }

    @Test
    @DisplayName("Deve enviar aviso com sucesso para um utilizador conectado")
    void shouldSendWarningSuccessfully() {
        sseNotificationService.addEmitter(userId);

        assertDoesNotThrow(() -> sseNotificationService.sendInactivityWarning(userId));

        @SuppressWarnings("unchecked")
        Map<UUID, SseEmitter> activeEmitters = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "activeEmitters");
        assertNotNull(activeEmitters);
        assertTrue(activeEmitters.containsKey(userId), "O utilizador deve continuar no mapa após o envio com sucesso");
    }
}
