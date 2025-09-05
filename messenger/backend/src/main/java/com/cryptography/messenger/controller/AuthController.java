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
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    private ResponseEntity<User> register(@RequestBody AuthRequest authRequest) {
        User created = userService.registerUser(authRequest.getUsername(), authRequest.getPassword());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    private ResponseEntity<String> login(@RequestBody AuthRequest authRequest) {
        AuthResponse success = userService.login(authRequest);
        return ResponseEntity.ok(success.getToken());
    }
}
