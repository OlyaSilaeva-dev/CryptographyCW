package com.cryptography.frontend.entity;

import com.cryptography.frontend.entity.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String chatId;
    private String senderId;
    private String recipientId;
    private byte[] message;
    private String fileName;
    private boolean isFile;
    private MessageStatus status;
    private String timestamp;
}