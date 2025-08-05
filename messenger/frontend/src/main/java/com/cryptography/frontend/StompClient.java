package com.cryptography.frontend;

import com.cryptography.frontend.entity.ChatMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class StompClient {
    private static StompSession stompSession;

    /**
     * Подключение STOMP клиента
     *
     * @param userId ID текущего пользователя
     * @param onMessage функция, вызываемая при получении нового сообщения
     */
    public static void connect(String userId, Consumer<ChatMessage> onMessage) {
        String url = "ws://localhost:8080/ws";

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
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
                onConnected(userId, stompSession, onMessage);
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }
        });
    }

    private static void onConnected(String userId, StompSession stompSession, Consumer<ChatMessage> onMessage) {
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

        stompSession.subscribe("/topic/user." + userId + "/key-exchange", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Received message (key): " + payload.toString());
            }
        });
    }

    private static void onError(Throwable error) {
        System.err.println("Error connecting: " + error.getMessage());
    }
}
