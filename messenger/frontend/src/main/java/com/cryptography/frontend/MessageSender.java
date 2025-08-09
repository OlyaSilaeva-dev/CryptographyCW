package com.cryptography.frontend;

import com.cryptography.frontend.dto.KeyParams;
import com.cryptography.frontend.entity.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class MessageSender {
    private static final String BASE_URL = "http://localhost:8080/api/v1/messages";

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void sendChatMessage(ChatMessage message) throws Exception {
        String json = toJson(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/send-message"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public static void sendPublicKeyMessage(ChatMessage message) throws Exception {
        String json = toJson(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/save-public-key"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public static KeyParams getKeyParams(String senderId, String recipientId) throws Exception {
        String encodedSenderId = URLEncoder.encode(senderId, StandardCharsets.UTF_8);
        String encodedRecipientId = URLEncoder.encode(recipientId, StandardCharsets.UTF_8);

        String url = String.format("%s/get-params?senderId=%s&recipientId=%s",
                BASE_URL, encodedSenderId, encodedRecipientId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseJsonToKeyParams(response.body());
        } else {
            throw new RuntimeException("Ошибка запроса: " + response.statusCode());
        }
    }

    public static byte[] getPublicKey(String senderId, String recipientId) throws Exception {
        String encodedSenderId = URLEncoder.encode(senderId, StandardCharsets.UTF_8);
        String encodedRecipientId = URLEncoder.encode(recipientId, StandardCharsets.UTF_8);

        String url = String.format("%s/get-public-key?senderId=%s&recipientId=%s",
                BASE_URL, encodedSenderId, encodedRecipientId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            byte[] body = response.body();
            if (body != null && "null".equals(new String(body, StandardCharsets.UTF_8))) {
                return body;
            }
            return null;
        } else {
            throw new RuntimeException("Ошибка запроса: " + response.statusCode());
        }
    }

    private static KeyParams parseJsonToKeyParams(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, KeyParams.class);
    }

    private static String toJson(ChatMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(message);
    }
}
