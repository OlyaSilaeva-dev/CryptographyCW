package com.cryptography.messenger.service;

import com.cryptography.messenger.enity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsumerService {

    private static final String CHAT_MESSAGE_TOPIC = "chat_message";
    private static final String CHAT_MESSAGE_GROUP = "chat_group";
    private static final String CHAT_KEY_EXCHANGE_TOPIC = "key-exchange";

    private final SimpMessagingTemplate messagingTemplate;

    public ConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = CHAT_MESSAGE_TOPIC, groupId = CHAT_MESSAGE_GROUP)
    public void consume(ConsumerRecord<String, ChatMessage> consumerRecord) {
        ChatMessage message = consumerRecord.value();
        log.info("Получено из Кафка (чат) {}", message);;

        String destination = "/topic/user." + message.getRecipientId();

        messagingTemplate.convertAndSend(destination, message);
        log.info("Отправлено через WebSocket:, {}", destination);
    }

    @KafkaListener(topics = CHAT_KEY_EXCHANGE_TOPIC, groupId = CHAT_MESSAGE_GROUP)
    public void consumeKeyExchange(ConsumerRecord<String, ChatMessage> consumerRecord) {
        ChatMessage message = consumerRecord.value();
        log.info("Получено из Кафка (обмен ключами) {}", message);

        String destination = "/topic/user." + message.getRecipientId() + "/key-exchange";

        messagingTemplate.convertAndSend(destination, message);
        log.info("Отправлено через WebSocket (ключи):, {}", destination);
    }
}
