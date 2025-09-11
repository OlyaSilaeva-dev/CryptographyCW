package com.cryptography.frontend.context;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, String> tokens = new HashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setToken(String userId, String token) {
        tokens.put(userId, token);
    }

    public String getToken(String userId) {
        return tokens.get(userId);
    }
}

