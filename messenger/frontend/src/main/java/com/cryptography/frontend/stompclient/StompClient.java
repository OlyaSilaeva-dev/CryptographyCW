package com.cryptography.frontend.stompclient;

import com.cryptography.frontend.dto.ChatDTO;
import com.cryptography.frontend.dto.NewChatDTO;
import com.cryptography.frontend.dto.UserDTO;
import com.cryptography.frontend.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class StompClient {
    private static StompSession stompSession;

    /**
     * Подключение STOMP клиента
     *
     * @param userId    ID текущего пользователя
     * @param onMessage функция, вызываемая при получении нового сообщения
     */
    public static void connect(String userId, Consumer<ChatMessage> onMessage, Consumer<ChatMessage> onPublicKeyReceived,
                               Consumer<UserDTO> onUserAdded, Consumer<ChatDTO> onChatAdded, Consumer<String> onChatRemoved) {
        String url = "ws://localhost:8080/ws";

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setInboundMessageSizeLimit(1024 * 1024);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                onError(exception);
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                log.info("Connected to server");
                onConnected(userId, stompSession, onMessage, onPublicKeyReceived, onUserAdded, onChatAdded, onChatRemoved);
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
        });
    }

    private static void onConnected(String userId, StompSession stompSession, Consumer<ChatMessage> onMessage, Consumer<ChatMessage> onPublicKeyReceived,
                                    Consumer<UserDTO> onContactAdded, Consumer<ChatDTO> onChatAdded, Consumer<String> onChatRemoved) {
        stompSession.subscribe("/topic/user." + userId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                log.info("Received message: {}", message);
                onMessage.accept(message);
            }
        });

        stompSession.subscribe("/topic/users", new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return UserDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                UserDTO userDTO = (UserDTO) payload;
                log.info("Новый пользователь: {}",userDTO);
                onContactAdded.accept(userDTO);
            }
        });

        stompSession.subscribe("/topic/chats/add", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatDTO chatDTO = (ChatDTO) payload;
                log.info("Новый чат: {}", chatDTO);
                onChatAdded.accept(chatDTO);
            }
        });

        stompSession.subscribe("/topic/chats/delete", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Map<String, Object> map = (Map<String, Object>) payload;
                String chatId = (String) map.get("chatId");
                log.info("Чат {} удалён", chatId);
                onChatRemoved.accept(chatId);
            }

        });

        stompSession.subscribe("/topic/user." + userId + "/key-exchange", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                log.info("Received message (key): {}",payload.toString());
                onPublicKeyReceived.accept(message);
            }
        });
    }

    private static void onError(Throwable error) {
        log.error("Error connecting: {}", error.getMessage());
    }
}
