package com.petfoodstore.controller;

import com.petfoodstore.dto.MessageResponse;
import com.petfoodstore.dto.NotificationDTO;
import com.petfoodstore.entity.User;
import com.petfoodstore.service.NotificationService;
import com.petfoodstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Get user notifications
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<NotificationDTO> notifications = notificationService.getUserNotifications(user, unreadOnly);

            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get unread notification count
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Long count = notificationService.getUnreadCount(user);

            return ResponseEntity.ok(count);

        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }

    // Mark notification as read
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(@PathVariable Long id,
                                        Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            notificationService.markAsRead(id, user);

            return ResponseEntity.ok(new MessageResponse("Notification marked as read"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            notificationService.markAllAsRead(user);

            return ResponseEntity.ok(new MessageResponse("All notifications marked as read"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}