package com.cryptography.frontend.apiclient;

import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.KeyParams;
import com.cryptography.frontend.entity.ChatMessage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static com.cryptography.frontend.apiclient.ApiClientUtils.*;

public class MessageSender {
    private static final String BASE_URL = "http://localhost:8080/api/v1/messages";

    public static void sendChatMessage(ChatMessage message) throws Exception {
        String token = SessionManager.getInstance().getToken(message.getSenderId());
        String json = toJson(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/send-message"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public static void sendPublicKeyMessage(ChatMessage message) throws Exception {
        String token = SessionManager.getInstance().getToken(message.getSenderId());
        String json = toJson(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/save-public-key"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public static KeyParams getKeyParams(String senderId, String recipientId) throws Exception {
        String encodedSenderId = URLEncoder.encode(senderId, StandardCharsets.UTF_8);
        String encodedRecipientId = URLEncoder.encode(recipientId, StandardCharsets.UTF_8);
        String token = SessionManager.getInstance().getToken(senderId);

        String url = String.format("%s/get-params?senderId=%s&recipientId=%s",
                BASE_URL, encodedSenderId, encodedRecipientId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return fromJson(response.body(), KeyParams.class);
        } else {
            throw new RuntimeException("Ошибка запроса: " + response.statusCode());
        }
    }

    public static byte[] getPublicKey(String senderId, String recipientId) throws Exception {
        String encodedSenderId = URLEncoder.encode(senderId, StandardCharsets.UTF_8);
        String encodedRecipientId = URLEncoder.encode(recipientId, StandardCharsets.UTF_8);
        String token = SessionManager.getInstance().getToken(senderId);

        String url = String.format("%s/get-public-key?senderId=%s&recipientId=%s",
                BASE_URL, encodedSenderId, encodedRecipientId);

        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Ошибка запроса: " + response.statusCode());
        }
    }
}
