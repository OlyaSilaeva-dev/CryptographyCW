package com.cryptography.messenger.controller;

import com.cryptography.messenger.enity.ChatMessage;
import com.cryptography.messenger.service.ProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private static final String CHAT_MESSAGE_TOPIC = "chat_message";
    private static final String CHAT_KEY_EXCHANGE_TOPIC = "key-exchange";
    private final ProducerService producer;

    public MessageController(ProducerService producerService) {
        this.producer = producerService;
    }

//    @GetMapping
//    public ResponseEntity<Iterable<ChatMessage>> getChatMessages() { //история сообщений
//
//    }

    @PostMapping("/send-message")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessage message) {
        producer.send(CHAT_MESSAGE_TOPIC, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/key-exchange")
    public ResponseEntity<Void> sendPublicKey(@RequestBody ChatMessage message) {
        producer.send(CHAT_KEY_EXCHANGE_TOPIC, message);
        return ResponseEntity.ok().build();
    }
}
