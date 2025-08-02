package com.cryptography.frontend.entity;

import lombok.Data;

@Data
public class ChatMessage {
    private String chatId;
    private String senderId;
    private String recipientId;
    private String message;
    private MessageStatus status;
}