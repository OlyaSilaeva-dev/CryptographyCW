package com.cryptography.frontend.apiclient;

import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.UserDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

import static com.cryptography.frontend.apiclient.ApiClientUtils.*;

public class UsersClient {

    private static final String BASE_URL = "http://localhost:8080/api/v1/users";
    public static List<UserDTO> getUsers(String id) throws Exception {

        String token = SessionManager.getInstance().getToken(id);
        if (token == null) {
            throw new RuntimeException("JWT токен не установлен");
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/all"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        if (response.body() == null || response.body().isBlank()) {
            return Collections.emptyList();
        }

        return mapper.readValue(response.body(), new TypeReference<>() {});
    }
}
