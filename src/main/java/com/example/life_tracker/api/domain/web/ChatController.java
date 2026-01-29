package com.example.life_tracker.api.domain.web;

import com.example.life_tracker.api.domain.domain.DailyInfo;
import com.example.life_tracker.api.domain.domain.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    String generation(@RequestParam(value = "userPrompt")String userPrompt) {
        return this.chatService.handleUserMessage(userPrompt);
    }
}
