package com.cryptography.frontend.context;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private String token;
    private String userId;
    private String userName;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }
}

