package com.cryptography.frontend;

import com.cryptography.frontend.entity.ChatMessage;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;

public class ChatWebSocketClient {

    private static final String WEBSOCKET_URL = "ws://localhost:8080/ws"; // точка входа

    public void connect(String userId) {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Connected to WebSocket");

                // Подписка на сообщения
                session.subscribe("/user/" + userId + "/queue/messages", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        ChatMessage msg = (ChatMessage) payload;
                        System.out.println("Получено сообщение: " + msg.getMessage());
                        // Тут можно обновить GUI JavaFX
                    }
                });

                // Подписка на ключи
                session.subscribe("/user/" + userId + "/queue/key-exchange", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        ChatMessage msg = (ChatMessage) payload;
                        System.out.println("Получен ключ: " + msg.getMessage());
                    }
                });
            }
        };

        stompClient.connect(WEBSOCKET_URL, sessionHandler);
    }

    public interface ChatMessageHandler {
        void handle(ChatMessage message);
    }
}
