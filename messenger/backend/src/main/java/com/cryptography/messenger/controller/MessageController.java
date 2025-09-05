package com.cryptography.messenger.controller;

import com.cryptography.messenger.dto.KeyParams;
import com.cryptography.messenger.enity.ChatMessage;
import com.cryptography.messenger.service.ProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private static final String CHAT_MESSAGE_TOPIC = "chat_message";
    private static final String CHAT_KEY_EXCHANGE_TOPIC = "key-exchange";
    private final ProducerService producer;
    private final RedisTemplate<String, byte[]> redis;

    public static final String DEFAULT_P = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF";

    public static final String DEFAULT_G = "2";

    public MessageController(ProducerService producerService, RedisTemplate<String, byte[]> redis) {
        this.producer = producerService;
        this.redis = redis;
    }

    @PostMapping("/send-message")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessage message) {
        producer.send(CHAT_MESSAGE_TOPIC, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/save-public-key")
    public ResponseEntity<Void> sendPublicKey(@RequestBody ChatMessage message) {
        String keyPrefix = "dh:" + message.getSenderId() + ":" + message.getRecipientId();
        redis.opsForValue().set(keyPrefix + ":public-key", message.getMessage());
        producer.send(CHAT_KEY_EXCHANGE_TOPIC, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-params")
    public ResponseEntity<KeyParams> getParams(@RequestParam String senderId,
                                               @RequestParam String recipientId) {
        return ResponseEntity.ok(new KeyParams(DEFAULT_P, DEFAULT_G));
    }

    @GetMapping("/get-public-key")
    public ResponseEntity<byte[]> getPublicKey(@RequestParam String senderId,
                                               @RequestParam String recipientId) {
        String keyPrefix = "dh:" + recipientId + ":" + senderId;
        byte[] publicKey = redis.opsForValue().get(keyPrefix + ":public-key");
        log.info("get public key: {}", publicKey);
        if (publicKey == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(publicKey);
    }
}
