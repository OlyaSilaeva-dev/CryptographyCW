package com.cryptography.frontend.controller;

import com.cryptography.frontend.MessageSender;
import com.cryptography.frontend.StompClient;
import com.cryptography.frontend.algorithms.DiffieHellman;
import com.cryptography.frontend.algorithms.MacGuffin.MacGuffin;
import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.algorithms.symmetricCipherContext.SymmetricCipherContext;
import com.cryptography.frontend.dto.KeyParams;
import com.cryptography.frontend.entity.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class ChatController {
    @FXML private ListView<String> contactsView;
    @FXML private ListView<String> listView;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private DiffieHellman diffieHellman;
    private String senderId;
    private String recipientId = null;

    private SymmetricCipherContext symmetricCipherContext;
    private final Map<String, List<String>> messageHistory = new HashMap<>();
    private final Map<String, BigInteger> sharedSecrets = new HashMap<>();

    private boolean isSharedSecretEstablished() {
        return recipientId != null
                && sharedSecrets.containsKey(recipientId)
                && sharedSecrets.get(recipientId) != null;
    }

    private void onMessageReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            String from = msg.getSenderId();
            String contact = from.equals(senderId) ? msg.getRecipientId() : from;

            byte[] decryptedBytes = symmetricCipherContext.decrypt(msg.getMessage());
            String displayText = from + ": " + new String(decryptedBytes, StandardCharsets.UTF_8);
            appendMessage(contact, displayText);

            if (!contactsView.getItems().contains(contact)) {
                contactsView.getItems().add(contact);
            }

            if (contact.equals(recipientId)) {
                updateMessageList();
            }
            log.debug("Received message: {}", msg);
        });
    }

    private void onPublicKeyReceived(ChatMessage msg) {
        Platform.runLater(() -> {
           String contactId = msg.getSenderId();
           byte[] publicKey = msg.getMessage();

            if (diffieHellman != null && contactId.equals(recipientId)) {
               BigInteger receivedPublicKey = new BigInteger(1, publicKey);
               BigInteger sharedSecret = diffieHellman.computeSharedSecret(receivedPublicKey);

               log.debug("Shared secret with " + contactId);
               sharedSecrets.put(contactId, sharedSecret);
           }

            symmetricCipherContext = new SymmetricCipherContext(
                    new MacGuffin(),
                    sharedSecrets.get(recipientId).toByteArray(),
                    EncryptionMode.ECB,
                    PaddingMode.PKCS7,
                    new byte[0]
            );

            updateMessageList();
        });
    }

    private void appendMessage(String contact, String message) {
        messageHistory.putIfAbsent(contact, new ArrayList<>());
        messageHistory.get(contact).add(message);

        if (Objects.equals(recipientId, contact)) {
            updateMessageList();
        }
    }

    private void updateMessageList() {
        listView.getItems().clear();
        listView.getItems().addAll(messageHistory.getOrDefault(recipientId, List.of()));
    }

    public void init(String senderId) {
        this.senderId = senderId;

        StompClient.connect(senderId, this::onMessageReceived, this::onPublicKeyReceived);

        List<String> users = new ArrayList<>(List.of("user1", "user2", "user3", "user4"));
        users.remove(senderId);
        contactsView.getItems().addAll(users);

        contactsView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                recipientId = newVal;

                if (sharedSecrets.containsKey(newVal)) {
                    updateMessageList();
                    return;
                }

                KeyParams keyParams = new KeyParams();
                try {
                    keyParams = MessageSender.getKeyParams(senderId, recipientId);
                    log.debug("keyParams: {}", keyParams);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

                if (keyParams != null) {
                    BigInteger p = new BigInteger(keyParams.getP(), 16);
                    BigInteger g = new BigInteger(keyParams.getG(), 16);
                    diffieHellman = new DiffieHellman(p, g);

                    try {
                        MessageSender.sendPublicKeyMessage(
                                ChatMessage.builder()
                                        .senderId(senderId)
                                        .recipientId(recipientId)
                                        .message(bigIntToUnsignedBytes(diffieHellman.getPublicKey()))
                                        .timestamp(LocalDateTime.now().toString())
                                        .build());
                        log.debug("User {} send public key to {}", senderId, recipientId);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }

                    javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            byte[] receivedPublicKey = MessageSender.getPublicKey(senderId, recipientId);
                            if (receivedPublicKey != null) {
                                BigInteger sharedSecret = diffieHellman.computeSharedSecret(new BigInteger(1, receivedPublicKey));
                                sharedSecrets.put(recipientId, sharedSecret);
                                log.debug("Shared secret with {}", recipientId);

                                // Создаем symmetricCipherContext в UI-потоке
                                Platform.runLater(() -> {
                                    symmetricCipherContext = new SymmetricCipherContext(
                                            new MacGuffin(),
                                            sharedSecret.toByteArray(),
                                            EncryptionMode.ECB,
                                            PaddingMode.PKCS7,
                                            new byte[0]
                                    );
                                    updateMessageList();
                                });
                            }
                            return null;
                        }
                    };
                    new Thread(task).start();

                }
            }
        });

        sendButton.setOnAction(event -> {
            String text = messageField.getText().trim();
            if (text.isEmpty() || recipientId == null) return;

            if (!isSharedSecretEstablished()) {
                appendMessage(recipientId, "Ошибка: общий секрет не установлен, дождитесь обмена ключами.");
                return;
            }

            ChatMessage msg = ChatMessage.builder()
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .message(symmetricCipherContext.encrypt(text.getBytes()))
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            try {
                MessageSender.sendChatMessage(msg);
                appendMessage(recipientId, "Вы: " + text);
                messageField.clear();
            } catch (Exception e) {
                appendMessage(recipientId, "Ошибка: " + e.getMessage());
            }
        });
    }

    public static byte[] bigIntToUnsignedBytes(BigInteger value) {
        byte[] signed = value.toByteArray();
        if (signed[0] == 0) {
            return Arrays.copyOfRange(signed, 1, signed.length);
        }
        return signed;
    }
}
