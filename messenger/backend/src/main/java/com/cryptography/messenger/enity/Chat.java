package com.cryptography.messenger.enity;

import com.cryptography.messenger.enity.enums.EncryptionMode;
import com.cryptography.messenger.enity.enums.PaddingMode;
import com.cryptography.messenger.enity.enums.SymmetricCipherEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "chats")
@NoArgsConstructor
@AllArgsConstructor
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "first_user_id", nullable = false)
    private User firstUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "second_user_id", nullable = false)
    private User secondUser;

    @Column(name = "symmetric_cipher")
    @Enumerated(EnumType.STRING)
    private SymmetricCipherEnum symmetricCipher;

    @Column(name = "encryption_mode")
    @Enumerated(EnumType.STRING)
    private EncryptionMode encryptionMode;

    @Column(name = "padding_mode")
    @Enumerated(EnumType.STRING)
    private PaddingMode paddingMode;

    @Column(name = "iv")
    private byte[] iv;
}
