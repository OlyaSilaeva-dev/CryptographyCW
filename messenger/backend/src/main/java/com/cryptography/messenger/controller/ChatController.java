package com.cryptography.messenger.controller;

import com.cryptography.messenger.dto.ChatDTO;
import com.cryptography.messenger.dto.NewChatDTO;
import com.cryptography.messenger.service.ChatService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/add")
    public ResponseEntity<Void> addChat(@RequestBody NewChatDTO chatDTO) {
        chatService.save(chatDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<List<ChatDTO>> getChatsByUserId(@PathVariable("id") String userId) {
        return ResponseEntity.ok(chatService.getChatsByUserId(userId));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteChat(@RequestParam String chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.ok().build();
    }
}
