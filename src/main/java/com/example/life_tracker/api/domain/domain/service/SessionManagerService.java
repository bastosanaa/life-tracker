package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.model.SessionState;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class SessionManagerService {

    private Cache<UUID, SessionState> sessionCache;

    private Consumer<UUID> onWarningCallback;
    private Consumer<UUID> onExpirationCallback;

    @PostConstruct
    public void init() {
        this.sessionCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(20)) //TODO: CHANGE TO 10MIN
                .scheduler(Scheduler.systemScheduler())
                .removalListener((UUID key, SessionState state, RemovalCause cause) -> {
                    if (cause == RemovalCause.EXPIRED) {
                        handleExpiration(key, state);
                    }
                })
                .build();
    }

    public void setCallbacks(Consumer<UUID> onWarning, Consumer<UUID> onExpiration) {
        this.onWarningCallback = onWarning;
        this.onExpirationCallback = onExpiration;
    }

    public void keepAlive(UUID userId) {
        sessionCache.put(userId, SessionState.ACTIVE);
    }

    public boolean hasActiveSession(UUID userId) {
        return sessionCache.getIfPresent(userId) != null;
    }

    public void invalidate(UUID userId) {
        sessionCache.invalidate(userId);
    }

    private void handleExpiration(UUID userId, SessionState state) {
        log.info("Time cycle expired for user {} in state {}", userId, state);

        switch (state) {
            case ACTIVE -> {
                if (onWarningCallback != null) onWarningCallback.accept(userId);
                sessionCache.put(userId, SessionState.WARNED);
            }
            case WARNED -> {
                if (onExpirationCallback != null) onExpirationCallback.accept(userId);
                sessionCache.invalidate(userId);
            }
        }
    }
}
