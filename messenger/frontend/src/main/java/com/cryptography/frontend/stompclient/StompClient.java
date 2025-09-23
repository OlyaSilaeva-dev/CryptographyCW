package com.cryptography.frontend.stompclient;

import com.cryptography.frontend.dto.UserDTO;
import com.cryptography.frontend.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
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
    public static void connect(String userId, Consumer<ChatMessage> onMessage, Consumer<ChatMessage> onPublicKeyReceived, Consumer<UserDTO> onContactAdded) {
        String url = "ws://localhost:8080/ws";

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setInboundMessageSizeLimit(1024 * 1024);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                onError(exception);
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                System.out.println("Connected to server");
                onConnected(userId, stompSession, onMessage, onPublicKeyReceived, onContactAdded);
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
        });
    }

    private static void onConnected(String userId, StompSession stompSession, Consumer<ChatMessage> onMessage, Consumer<ChatMessage> onPublicKeyReceived,
                                    Consumer<UserDTO> onContactAdded) {
        stompSession.subscribe("/topic/user." + userId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                System.out.println("Received message: " + message);
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
                log.info("Новый пользователь: " + userDTO);
                onContactAdded.accept(userDTO);
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
                System.out.println("Received message (key): " + payload.toString());
                onPublicKeyReceived.accept(message);
            }
        });
    }

    private static void onError(Throwable error) {
        System.err.println("Error connecting: " + error.getMessage());
    }
}
