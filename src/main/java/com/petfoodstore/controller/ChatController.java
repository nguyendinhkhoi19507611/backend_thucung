package com.petfoodstore.controller;

import com.petfoodstore.dto.*;
import com.petfoodstore.entity.ChatMessage;
import com.petfoodstore.entity.ChatRoom;
import com.petfoodstore.entity.User;
import com.petfoodstore.service.ChatService;
import com.petfoodstore.service.UserService;
import com.petfoodstore.service.WebSocketMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketMessagingService messagingService;

    // WebSocket endpoints
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId,
                            @Payload ChatMessageDTO messageDTO,
                            Principal principal) {
        try {
            User sender = userService.findByUsername(principal.getName());

            ChatMessageDTO sentMessage = chatService.sendMessage(
                    roomId,
                    sender,
                    messageDTO.getContent(),
                    ChatMessage.MessageType.valueOf(messageDTO.getMessageType())
            );

            log.info("WebSocket message sent in room {}: {}", roomId, messageDTO.getContent());

        } catch (Exception e) {
            log.error("Error sending WebSocket message: ", e);
        }
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void handleTyping(@DestinationVariable String roomId,
                             @Payload TypingIndicatorDTO typingIndicator,
                             Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());

            typingIndicator.setUserId(user.getId());
            typingIndicator.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername());
            typingIndicator.setRoomId(roomId);

            messagingService.sendTypingIndicator(roomId, typingIndicator);

        } catch (Exception e) {
            log.error("Error handling typing indicator: ", e);
        }
    }

    @MessageMapping("/chat.joinRoom/{roomId}")
    @SendToUser("/queue/room.joined")
    public WebSocketMessage joinRoom(@DestinationVariable String roomId, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            log.info("User {} joined room {}", user.getUsername(), roomId);

            return new WebSocketMessage("ROOM_JOINED", roomId);

        } catch (Exception e) {
            log.error("Error joining room: ", e);
            return new WebSocketMessage("ERROR", "Failed to join room");
        }
    }

    @MessageMapping("/chat.leaveRoom/{roomId}")
    public void leaveRoom(@DestinationVariable String roomId, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            log.info("User {} left room {}", user.getUsername(), roomId);

        } catch (Exception e) {
            log.error("Error leaving room: ", e);
        }
    }
}
