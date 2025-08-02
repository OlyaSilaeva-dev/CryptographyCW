package com.cryptography.frontend;

import com.cryptography.frontend.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.OutputStream;

public class ChatApp extends Application {
    private final String currentUserId = "user123"; // <-- текущий пользователь
    private final String recipientId = "user456";   // <-- получатель

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Stage primaryStage) {
        ListView<String> messageList = new ListView<>();
        TextField input = new TextField();
        Button sendButton = new Button("Send");

        sendButton.setOnAction(event -> {
            String text = input.getText();
            if (!text.isEmpty()) {
                ChatMessage msg = new ChatMessage();
                msg.setSenderId(currentUserId);
                msg.setRecipientId(recipientId);
                msg.setMessage(text);
                sendMessage(msg);
                input.clear();
            }
        });

        VBox root = new VBox(10, messageList, new HBox(10, input, sendButton));
        root.setPrefSize(400, 300);
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("JavaFX Chat");
        primaryStage.show();

        // WebSocket подключение
        ChatWebSocketClient client = new ChatWebSocketClient();
        client.connect(currentUserId
//                , msg -> {
//            Platform.runLater(() -> messageList.getItems().add("📩: " + msg.getMessage()));
//        }, keyMsg -> {
//            Platform.runLater(() -> messageList.getItems().add("🔐 Ключ: " + keyMsg.getContent()));
//        }
        );
    }

    private void sendMessage(ChatMessage message) {
        try {
            URL url = new URL("http://localhost:8080/api/v1/messages/send-message");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = mapper.writeValueAsString(message);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            int responseCode = conn.getResponseCode();
            System.out.println("POST статус: " + responseCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
