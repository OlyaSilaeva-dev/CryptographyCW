package com.cryptography.frontend.apiclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

class ApiClientUtils {
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }
}
