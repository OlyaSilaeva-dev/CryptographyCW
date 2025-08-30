package com.cryptography.messenger.enity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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
