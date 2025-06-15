package com.petfoodstore.service;

import com.petfoodstore.dto.*;
import com.petfoodstore.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WebSocketMessagingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Track online users
    private final Map<Long, String> onlineUsers = new ConcurrentHashMap<>();

    // Send message to specific room
    public void sendMessageToRoom(String roomId, ChatMessageDTO message) {
        WebSocketMessage wsMessage = new WebSocketMessage("MESSAGE", message, roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, wsMessage);

        log.debug("Message sent to room {}: {}", roomId, message.getContent());
    }

    // Send notification to specific user
    public void sendNotificationToUser(Long userId, NotificationDTO notification) {
        WebSocketMessage wsMessage = new WebSocketMessage("NOTIFICATION", notification);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", wsMessage);

        log.debug("Notification sent to user {}: {}", userId, notification.getTitle());
    }

    // Send typing indicator to room
    public void sendTypingIndicator(String roomId, TypingIndicatorDTO typingIndicator) {
        WebSocketMessage wsMessage = new WebSocketMessage("TYPING", typingIndicator, roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, wsMessage);
    }

    // Broadcast online status update
    public void broadcastOnlineStatus(OnlineStatusDTO statusUpdate) {
        WebSocketMessage wsMessage = new WebSocketMessage("USER_STATUS", statusUpdate);
        messagingTemplate.convertAndSend("/topic/online-status", wsMessage);
    }

    // Send order update to user
    public void sendOrderUpdate(Long userId, Order order) {
        WebSocketMessage wsMessage = new WebSocketMessage("ORDER_UPDATE", order);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/orders", wsMessage);

        log.debug("Order update sent to user {}: order #{}", userId, order.getOrderNumber());
    }

    // Send admin notification (to all admin/employee users)
    public void sendAdminNotification(NotificationDTO notification) {
        WebSocketMessage wsMessage = new WebSocketMessage("ADMIN_NOTIFICATION", notification);
        messagingTemplate.convertAndSend("/topic/admin/notifications", wsMessage);

        log.debug("Admin notification broadcast: {}", notification.getTitle());
    }

    // User connection management
    public void userConnected(Long userId, String sessionId) {
        onlineUsers.put(userId, sessionId);

        OnlineStatusDTO statusUpdate = new OnlineStatusDTO();
        statusUpdate.setUserId(userId);
        statusUpdate.setStatus("ONLINE");
        statusUpdate.setLastSeen(java.time.LocalDateTime.now());

        broadcastOnlineStatus(statusUpdate);

        log.info("User {} connected with session {}", userId, sessionId);
    }

    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);

        OnlineStatusDTO statusUpdate = new OnlineStatusDTO();
        statusUpdate.setUserId(userId);
        statusUpdate.setStatus("OFFLINE");
        statusUpdate.setLastSeen(java.time.LocalDateTime.now());

        broadcastOnlineStatus(statusUpdate);

        log.info("User {} disconnected", userId);
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    public Map<Long, String> getOnlineUsers() {
        return Map.copyOf(onlineUsers);
    }
}