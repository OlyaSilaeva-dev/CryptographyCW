package com.cryptography.messenger.enity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChatMessage {
    private String chatId;
    private String senderId;
    private String recipientId;
    private String message;
    private MessageStatus status;
}
