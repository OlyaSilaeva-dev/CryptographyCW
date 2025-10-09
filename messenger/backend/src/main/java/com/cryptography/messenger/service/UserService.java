package com.cryptography.messenger.service;

import com.cryptography.messenger.dto.AuthResponse;
import com.cryptography.messenger.dto.UserDTO;
import com.cryptography.messenger.enity.User;
import com.cryptography.messenger.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void registerUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        try {
            userRepository.save(user);
            UserDTO userDTO = new UserDTO(user);
            messagingTemplate.convertAndSend("/topic/users", userDTO);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Имя пользователя уже используется");
        }
    }

    public AuthResponse login(String username, String password) {
        log.info("Login attempt: {}", username);
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.error("Неверное имя пользователя или пароль");
            throw new IllegalArgumentException("Неверное имя пользователя или пароль");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getUsername());
        return new AuthResponse(user.getId().toString(), token);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
