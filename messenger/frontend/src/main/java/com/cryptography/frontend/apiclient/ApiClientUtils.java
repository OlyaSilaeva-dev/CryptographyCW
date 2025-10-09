package com.cryptography.frontend.apiclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

class ApiClientUtils {
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static final ObjectMapper mapper = new ObjectMapper();
    public static final String BEARER = "Bearer ";

    public static String toJson(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }
}
