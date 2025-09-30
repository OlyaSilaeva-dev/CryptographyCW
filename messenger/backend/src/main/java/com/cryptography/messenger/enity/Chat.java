package com.cryptography.messenger.enity;

import com.cryptography.messenger.enity.enums.EncryptionMode;
import com.cryptography.messenger.enity.enums.PaddingMode;
import com.cryptography.messenger.enity.enums.SymmetricCipher;
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

    @Enumerated(EnumType.STRING)
    private SymmetricCipher symmetricCipher;

    @Enumerated(EnumType.STRING)
    private EncryptionMode encryptionMode;

    @Enumerated(EnumType.STRING)
    private PaddingMode paddingMode;
}
