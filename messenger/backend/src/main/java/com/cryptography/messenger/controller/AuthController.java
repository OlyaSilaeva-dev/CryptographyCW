package com.cryptography.messenger.controller;

import com.cryptography.messenger.dto.AuthRequest;
import com.cryptography.messenger.dto.AuthResponse;
import com.cryptography.messenger.enity.User;
import com.cryptography.messenger.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthRequest authRequest) {
        userService.registerUser(authRequest.getUsername(), authRequest.getPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        AuthResponse success = userService.login(authRequest);
        return ResponseEntity.ok(success);
    }
}
