package com.example.life_tracker.api.domain.web;

import com.example.life_tracker.api.domain.domain.service.JournalingService;
import com.example.life_tracker.api.domain.domain.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final JournalingService journalService;
    private final SseNotificationService sseNotificationService;

    private static final UUID DEFAULT_USER_ID = UUID.fromString("9cfd9fa2-110e-49a3-8148-65daa18d9c68");

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseNotificationService.addEmitter(DEFAULT_USER_ID);
    }

    @GetMapping(value = "/close")
    Flux<String> closeConversation() {
        return journalService.manuallyCloseConversation(DEFAULT_USER_ID);
    }

    @GetMapping
    Flux<String> generation(@RequestParam(value = "userPrompt")String userPrompt) {
        return this.journalService.handleUserMessage(DEFAULT_USER_ID, userPrompt);
    }
}
