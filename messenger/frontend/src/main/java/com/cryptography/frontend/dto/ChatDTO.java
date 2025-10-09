package com.cryptography.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {
    String chatId;
    UserDTO firstUser;
    UserDTO secondUser;
    String symmetricCipher;
    String encryptionMode;
    String paddingMode;
}
