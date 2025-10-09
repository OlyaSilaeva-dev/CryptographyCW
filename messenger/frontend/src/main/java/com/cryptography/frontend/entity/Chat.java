package com.cryptography.frontend.entity;

import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.algorithms.interfaces.SymmetricCipher;
import com.cryptography.frontend.dto.UserDTO;
import lombok.Data;

@Data
public class Chat {
    private Long id;
    private UserDTO firstUser;
    private UserDTO secondUser;
    private SymmetricCipher symmetricCipher;
    private EncryptionMode encryptionMode;
    private PaddingMode paddingMode;
}
