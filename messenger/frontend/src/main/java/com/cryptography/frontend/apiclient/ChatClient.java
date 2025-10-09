package com.cryptography.frontend.apiclient;

import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.ChatDTO;
import com.cryptography.frontend.dto.NewChatDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static com.cryptography.frontend.apiclient.ApiClientUtils.*;

public class ChatClient {
    private static final String BASE_URL = "http://localhost:8080/api/v1/chats";

    public static void addChat(NewChatDTO newChatDTO) throws Exception {
        String token = SessionManager.getInstance().getToken();
        if (token == null) throw new RuntimeException("JWT токен не установлен");
        String json = toJson(newChatDTO);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/add"))
                .header("Content-Type", "application/json")
                .header("Authorization", BEARER + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300 || response.statusCode() < 200) {
            throw new RuntimeException("Ошибка при создании чата: " + response.statusCode());
        }
    }

    public static void removeChat(String id) throws Exception {
        String token = SessionManager.getInstance().getToken();
        if (token == null) throw new RuntimeException("JWT токен не установлен");

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/delete?chatId=" + id))
                .header("Content-Type", "application/json")
                .header("Authorization", BEARER + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300 || response.statusCode() < 200) {
            throw new RuntimeException("Ошибка при удалении чата: " + response.body());
        }
    }

    public static List<ChatDTO> getChats(String id) throws Exception {
        String token = SessionManager.getInstance().getToken();
        if (token == null) throw new RuntimeException("JWT токен не установлен");
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/all/" + id))
                .header("Content-Type", "application/json")
                .header("Authorization", BEARER + token)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300 || response.statusCode() < 200) {
            throw new RuntimeException("Ошибка при получении списка чатов: " + response.statusCode() + " " + response.body());
        }
        return mapper.readValue(response.body(), new TypeReference<>() {});
    }
}
