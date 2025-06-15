package com.petfoodstore.controller;

import com.petfoodstore.service.WebSocketMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketController {

    @Autowired
    private WebSocketMessagingService messagingService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Extract user info from authentication
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            log.info("WebSocket connection established for user: {} with session: {}", username, sessionId);

            // You can track online users here if needed
            // messagingService.userConnected(userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            log.info("WebSocket connection closed for user: {} with session: {}", username, sessionId);

            // You can track offline users here if needed
            // messagingService.userDisconnected(userId);
        }
    }
}