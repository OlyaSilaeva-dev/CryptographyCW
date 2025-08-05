package com.cryptography.frontend;

import com.cryptography.frontend.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    public static void sendKeyExchangeMessage(ChatMessage message) throws Exception {
        String json = toJson(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/key-exchange"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private static String toJson(ChatMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(message);
    }
}
