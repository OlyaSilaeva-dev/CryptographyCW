package com.cryptography.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewChatDTO {
    private String firstUserId;
    private String secondUserId;
    private String symmetricCipher;
    private String encryptionMode;
    private String paddingMode;
}
