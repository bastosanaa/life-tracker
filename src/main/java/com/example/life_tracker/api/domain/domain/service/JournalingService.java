package com.example.life_tracker.api.domain.domain.service;

import com.example.life_tracker.api.domain.domain.PromptTemplates;
import com.example.life_tracker.api.domain.domain.ReplyMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalingService {

    private final SessionManagerService sessionManager;
    private final ChatService chatService;
    private final JournalingIngestionService ingestionService;
    private final SseNotificationService sseNotificationService;

    private static final int MAX_INTERACTIONS_BEFORE_SAVE = 1;

    private static final String CONVERSATION_ID = "user-default"; //TODO: adicionar id usuário logado

    @PostConstruct
    public void init() {
        sessionManager.setCallbacks(
            this::sendInactivityWarning,
                this::consolidateAndClose
        );
    }

    public Flux<String> handleUserMessage(UUID userId, String userMessage) {

        if (!canInteract(userId)) return Flux.just(ReplyMessages.JOURNAL_ALREADY_EXISTS);

        sessionManager.keepAlive(userId);

        if (isBelowInteractionsLimit(userId)) {
            consolidateAndClose(userId);
            return Flux.just(ReplyMessages.CONSOLIDATE);
        }

        return chatService.processMessage(userId, userMessage);

    }

    private boolean canInteract(UUID userId) {
        boolean hasActiveSession = sessionManager.hasActiveSession(userId);

        if (hasActiveSession) return true;

        boolean hasJournaled = ingestionService.hasJournaledToday(userId);
        return !hasJournaled;
    }


    private boolean isBelowInteractionsLimit(UUID userId) {
        int historySize = chatService.getCurrentHistorySize(userId);
        return historySize / 2 >= MAX_INTERACTIONS_BEFORE_SAVE;
    }

    private void sendInactivityWarning(UUID userId) {
        sseNotificationService.trySendInactivityWarning(userId);
    }

    private void consolidateAndClose(UUID userId) {
        log.info("Iniciando consolidação para usuário {}", userId);

        String history = chatService.getHistorySnapshot(userId);

        boolean isHistoryNotEmpty = !history.isEmpty();
        if (isHistoryNotEmpty) {
            ingestionService.ingest(history, userId);
            chatService.clearHistory(userId);
        }

        sessionManager.invalidate(userId);
        sseNotificationService.removeEmitter(userId);
    }



}
