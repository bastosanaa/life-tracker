package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.ReplyMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseNotificationService {

    private static final String EVENT_NAME_WARNING = "WARNING";
    private final Map<UUID, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(UUID userId) {
        // Long.MAX_VALUE means server doesn't close connection by time
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        setCleaningCallbacks(emitter, userId);

        activeEmitters.put(userId, emitter);
        log.info("Nova conexão SSE registrada para usuário: {}", userId);

        return emitter;
    };

    public void removeEmitter(UUID userId) {
        SseEmitter emitter = activeEmitters.remove(userId);
        boolean isUserConnected =  emitter != null;

        if (isUserConnected) {
            emitter.complete();
            log.info("Conexão SSE encerrada manualmente para usuário: {}", userId);
        }
    }

    public void trySendInactivityWarning(UUID userId) {
        SseEmitter emitter = activeEmitters.get(userId);

        boolean isUserConnected =  emitter != null;
        if (!isUserConnected) {
            log.debug("Tentativa de enviar aviso para usuário desconectado: {}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME_WARNING)
                    .data(ReplyMessages.INACTIVITY_WARNING));
            log.info("Aviso de inatividade enviado para usuário: {}", userId);
        } catch (Exception IOException) {
            log.warn("Falha ao enviar aviso SSE. Removendo usuário: {}", userId);
            disconnectUser(userId);
        }

    }

    private void setCleaningCallbacks(SseEmitter emitter, UUID userId) {
        emitter.onCompletion(() -> {
            log.debug("SSE completado para usuário: {}", userId);
            disconnectUser(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout para usuário: {}", userId);
            emitter.complete();
            disconnectUser(userId);
        });

        emitter.onError((e) -> {
            log.error("Erro na conexão SSE do usuário {}: {}", userId, e.getMessage());
            emitter.completeWithError(e);
            disconnectUser(userId);
        });
    }

    private void disconnectUser(UUID userId) {
        activeEmitters.remove(userId);
    }
}


