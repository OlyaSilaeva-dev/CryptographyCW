package com.cryptography.messenger.enity;

import lombok.Data;

@Data
public class Chat {
    private String id;
    private String firstUserId;
    private String secondUserId;
}
