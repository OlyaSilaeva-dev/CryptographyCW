package com.cryptography.messenger.service;

import com.cryptography.messenger.enity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProducerService {
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    public ProducerService(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, ChatMessage message) {
        kafkaTemplate.send(topic, message);
        log.info("Отправлено в Kafka: " + message);
    }
}
