package com.cryptography.frontend.controller;

import com.cryptography.frontend.MessageSender;
import com.cryptography.frontend.StompClient;
import com.cryptography.frontend.entity.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.*;

public class ChatController {
    @FXML private ListView<String> contactsView;
    @FXML private ListView<String> listView;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private String senderId;
    private String recipientId = null;

    private final Map<String, List<String>> messageHistory = new HashMap<>();

    private void onMessageReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            String from = msg.getSenderId();
            String contact = from.equals(senderId) ? msg.getRecipientId() : from;

            String displayText = from + ": " + msg.getMessage();
            appendMessage(contact, displayText);

            if (!contactsView.getItems().contains(contact)) {
                contactsView.getItems().add(contact);
            }

            if (contact.equals(recipientId)) {
                updateMessageList();
            }
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

        StompClient.connect(senderId, this::onMessageReceived);

        List<String> users = new ArrayList<>(List.of("user1", "user2", "user3", "user4"));
        users.remove(senderId);
        contactsView.getItems().addAll(users);

        contactsView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                recipientId = newVal;
                updateMessageList();
            }
        });

        sendButton.setOnAction(event -> {
            String text = messageField.getText().trim();
            if (text.isEmpty() || recipientId == null) return;

            ChatMessage msg = ChatMessage.builder()
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .message(text)
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
}
