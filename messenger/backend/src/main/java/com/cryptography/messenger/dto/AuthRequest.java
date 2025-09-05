package com.cryptography.messenger.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private final String username;
    private final String password;
}
