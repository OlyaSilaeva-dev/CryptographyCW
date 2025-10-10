package com.cryptography.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {
    private String chatId;
    private UserDTO firstUser;
    private UserDTO secondUser;
    private String symmetricCipher;
    private String encryptionMode;
    private String paddingMode;
    private byte[] iv;
}
