package com.cryptography.frontend.apiclient;

import com.cryptography.frontend.dto.AuthRequest;
import com.cryptography.frontend.dto.AuthResponse;
import com.cryptography.frontend.dto.SessionContext;
import com.cryptography.frontend.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.cryptography.frontend.apiclient.ApiClientUtils.fromJson;
import static com.cryptography.frontend.apiclient.ApiClientUtils.toJson;

public class AuthClient {
    private static final String BASE_URL = "http://localhost:8080/api/v1/auth";

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static AuthResponse login(AuthRequest request) throws Exception {
        String json = toJson(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return fromJson(response.body(), AuthResponse.class);
    }

    public static void register(AuthRequest request) throws Exception {
        String json = toJson(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка регистрации: " + response.statusCode() + " - " + response.body());
        }
    }
}
