package com.petfoodstore.controller;

import com.petfoodstore.dto.*;
import com.petfoodstore.entity.ChatRoom;
import com.petfoodstore.entity.User;
import com.petfoodstore.service.ChatService;
import com.petfoodstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatRestController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    // Create or get chat room
    @PostMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomDTO> createChatRoom(@RequestBody CreateChatRoomRequest request,
                                                      Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());

            ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(request.getRoomType());
            ChatRoomDTO room = chatService.createOrGetChatRoom(user, roomType, request.getSubject());

            return ResponseEntity.ok(room);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get user's chat rooms
    @GetMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatRoomDTO>> getUserChatRooms(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<ChatRoomDTO> rooms = chatService.getUserChatRooms(user);

            return ResponseEntity.ok(rooms);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get chat history
    @GetMapping("/rooms/{roomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable String roomId,
                                                               Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<ChatMessageDTO> messages = chatService.getChatHistory(roomId, user);

            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Close chat room
    @PutMapping("/rooms/{roomId}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> closeChatRoom(@PathVariable String roomId,
                                           Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            chatService.closeChatRoom(roomId, user);

            return ResponseEntity.ok(new MessageResponse("Chat room closed successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // Get unassigned rooms (Admin/Employee)
    @GetMapping("/rooms/unassigned")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<ChatRoomDTO>> getUnassignedRooms() {
        try {
            List<ChatRoomDTO> rooms = chatService.getUnassignedRooms();
            return ResponseEntity.ok(rooms);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Assign staff to room (Admin/Employee)
    @PutMapping("/rooms/{roomId}/assign")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ChatRoomDTO> assignStaffToRoom(@PathVariable String roomId,
                                                         Authentication authentication) {
        try {
            User staff = userService.findByUsername(authentication.getName());
            ChatRoomDTO room = chatService.assignStaffToRoom(roomId, staff);

            return ResponseEntity.ok(room);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get unread message count
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadMessageCount(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Long count = chatService.getUnreadMessageCount(user);

            return ResponseEntity.ok(count);

        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }

    // DTO for create room request
    public static class CreateChatRoomRequest {
        private String roomType = "SUPPORT";
        private String subject;

        public String getRoomType() { return roomType; }
        public void setRoomType(String roomType) { this.roomType = roomType; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
    }
}
