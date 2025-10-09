package com.cryptography.messenger.service;

import com.cryptography.messenger.dto.ChatDTO;
import com.cryptography.messenger.dto.NewChatDTO;
import com.cryptography.messenger.dto.UserDTO;
import com.cryptography.messenger.enity.Chat;
import com.cryptography.messenger.enity.User;
import com.cryptography.messenger.enity.enums.EncryptionMode;
import com.cryptography.messenger.enity.enums.PaddingMode;
import com.cryptography.messenger.enity.enums.SymmetricCipherEnum;
import com.cryptography.messenger.repository.ChatRepository;
import com.cryptography.messenger.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void save(NewChatDTO newChatDTO) {
        SymmetricCipherEnum symmetricCipher = SymmetricCipherEnum.valueOf(newChatDTO.getSymmetricCipher());
        EncryptionMode encryptionMode = EncryptionMode.valueOf(newChatDTO.getEncryptionMode());
        PaddingMode paddingMode = PaddingMode.valueOf(newChatDTO.getPaddingMode());

        User firstUser = userRepository.getUserById(Long.parseLong(newChatDTO.getFirstUserId()));
        User secondUser = userRepository.getUserById(Long.parseLong(newChatDTO.getSecondUserId()));

        if (firstUser == null || secondUser == null) {
            throw new IllegalArgumentException("Неверное имя первого или второго пользователя!");
        }

        Chat newChat = Chat.builder()
                .firstUser(firstUser)
                .secondUser(secondUser)
                .symmetricCipher(symmetricCipher)
                .encryptionMode(encryptionMode)
                .paddingMode(paddingMode)
                .build();

        chatRepository.save(newChat);
        ChatDTO chatDTO = ChatDTO.builder()
                .chatId(String.valueOf(newChat.getId()))
                .firstUser(new UserDTO(firstUser))
                .secondUser(new UserDTO(secondUser))
                .encryptionMode(String.valueOf(encryptionMode))
                .paddingMode(String.valueOf(paddingMode))
                .symmetricCipher(String.valueOf(symmetricCipher))
                .build();
        simpMessagingTemplate.convertAndSend("/topic/chats/add", chatDTO);
    }

    public void deleteChat(String chatId) {
        chatRepository.deleteById(Long.parseLong(chatId));
        simpMessagingTemplate.convertAndSend("/topic/chats/delete", chatId);
    }

    public List<ChatDTO> getChatsByUserId(String userId) {
        User user = userRepository.getUserById(Long.parseLong(userId));
        if (user == null) {
            throw new IllegalArgumentException("Пользователя не существует!");
        }

        List<Chat> chats = new ArrayList<>();
        chats.addAll(chatRepository.getChatsByFirstUser(user));
        chats.addAll(chatRepository.getChatsBySecondUser(user));

        return chats.stream()
                .map(chat -> ChatDTO.builder()
                        .chatId(String.valueOf(chat.getId()))
                        .firstUser(UserDTO.builder()
                                .id(String.valueOf(chat.getFirstUser().getId()))
                                .name(chat.getFirstUser().getUsername())
                                .build())
                        .secondUser(UserDTO.builder()
                                .id(String.valueOf(chat.getSecondUser().getId()))
                                .name(chat.getSecondUser().getUsername())
                                .build())
                        .symmetricCipher(chat.getSymmetricCipher().toString())
                        .encryptionMode(chat.getEncryptionMode().toString())
                        .paddingMode(chat.getPaddingMode().toString())
                        .build())
                .toList();
    }
}
