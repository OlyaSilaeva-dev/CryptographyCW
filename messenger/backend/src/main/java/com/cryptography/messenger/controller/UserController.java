package com.cryptography.messenger.controller;

import com.cryptography.messenger.dto.UserDTO;
import com.cryptography.messenger.enity.Users;
import com.cryptography.messenger.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getUsers() {
        List<Users> users = userService.findAll();
        List<UserDTO> dtos = users.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId().toString())
                        .name(user.getUsername())
                        .build()
                )
                .toList();
        log.info(dtos.toString());
        return ResponseEntity.ok(dtos);
    }
}
