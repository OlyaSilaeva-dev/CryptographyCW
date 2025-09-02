package com.cryptography.frontend.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReceivedFile {
    private String fileName;
    private byte[] fileData;
    private String senderId;
    private LocalDateTime receivedTime;

    public ReceivedFile(String fileName, byte[] fileData, String senderId) {
        this.fileName = fileName;
        this.fileData = fileData;
        this.senderId = senderId;
        this.receivedTime = LocalDateTime.now();
    }
}
