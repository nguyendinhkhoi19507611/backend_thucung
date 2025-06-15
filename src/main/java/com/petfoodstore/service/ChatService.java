package com.petfoodstore.service;

import com.petfoodstore.dto.ChatMessageDTO;
import com.petfoodstore.dto.ChatRoomDTO;
import com.petfoodstore.entity.ChatMessage;
import com.petfoodstore.entity.ChatRoom;
import com.petfoodstore.entity.User;
import com.petfoodstore.repository.ChatMessageRepository;
import com.petfoodstore.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ChatService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository roomRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketMessagingService messagingService;

    // Create or get existing chat room
    public ChatRoomDTO createOrGetChatRoom(User customer, ChatRoom.RoomType roomType, String subject) {
        // Check if there's an active room for this customer
        List<ChatRoom> activeRooms = roomRepository.findByCustomerOrderByLastMessageAtDesc(customer)
                .stream()
                .filter(room -> room.getStatus() == ChatRoom.RoomStatus.ACTIVE ||
                        room.getStatus() == ChatRoom.RoomStatus.WAITING)
                .collect(Collectors.toList());

        ChatRoom room;
        if (!activeRooms.isEmpty() && roomType == ChatRoom.RoomType.SUPPORT) {
            // Use existing support room
            room = activeRooms.get(0);
        } else {
            // Create new room
            room = new ChatRoom();
            room.setRoomId(generateRoomId());
            room.setCustomer(customer);
            room.setRoomType(roomType);
            room.setStatus(ChatRoom.RoomStatus.WAITING);
            room.setSubject(subject);
            room.setPriority(ChatRoom.Priority.NORMAL);
            room = roomRepository.save(room);

            log.info("Created new chat room: {} for customer: {}", room.getRoomId(), customer.getUsername());
        }

        return new ChatRoomDTO(room);
    }

    // Send message
    public ChatMessageDTO sendMessage(String roomId, User sender, String content, ChatMessage.MessageType messageType) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setTicketId(roomId);

        // Set receiver based on sender role
        if (sender.getRole() == User.UserRole.CUSTOMER) {
            if (room.getStaff() != null) {
                message.setReceiver(room.getStaff());
            }
        } else {
            message.setReceiver(room.getCustomer());
            // Assign staff to room if not assigned
            if (room.getStaff() == null) {
                room.setStaff(sender);
                room.setStatus(ChatRoom.RoomStatus.ACTIVE);
            }
        }

        message = messageRepository.save(message);

        // Update room last message time
        room.updateLastMessageTime();
        roomRepository.save(room);

        ChatMessageDTO messageDTO = new ChatMessageDTO(message);
        messageDTO.setRoomId(roomId);

        // Send via WebSocket
        messagingService.sendMessageToRoom(roomId, messageDTO);

        // Send notification to receiver
        if (message.getReceiver() != null) {
            notificationService.createChatNotification(message.getReceiver(), sender, content, roomId);
        }

        log.info("Message sent in room {}: {} -> {}", roomId, sender.getUsername(),
                message.getReceiver() != null ? message.getReceiver().getUsername() : "all");

        return messageDTO;
    }

    // Get chat history
    public List<ChatMessageDTO> getChatHistory(String roomId, User user) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        // Check if user has access to this room
        if (!hasRoomAccess(room, user)) {
            throw new RuntimeException("Access denied to chat room: " + roomId);
        }

        List<ChatMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(roomId);

        // Mark messages as read for the current user
        markMessagesAsRead(messages, user);

        return messages.stream()
                .map(msg -> {
                    ChatMessageDTO dto = new ChatMessageDTO(msg);
                    dto.setRoomId(roomId);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Get user's chat rooms
    public List<ChatRoomDTO> getUserChatRooms(User user) {
        List<ChatRoom> rooms = roomRepository.findRoomsByUser(user);

        return rooms.stream()
                .map(room -> {
                    ChatRoomDTO dto = new ChatRoomDTO(room);

                    // Add unread count
                    Long unreadCount = messageRepository.countUnreadMessagesInTicket(room.getRoomId(), user);
                    dto.setUnreadCount(unreadCount.intValue());

                    // Add last message
                    ChatMessage lastMessage = messageRepository.findLatestMessageInTicket(room.getRoomId());
                    if (lastMessage != null) {
                        dto.setLastMessage(new ChatMessageDTO(lastMessage));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Get unassigned support rooms (for admin/employee)
    public List<ChatRoomDTO> getUnassignedRooms() {
        List<ChatRoom> rooms = roomRepository.findUnassignedRoomsByStatus(ChatRoom.RoomStatus.WAITING);

        return rooms.stream()
                .map(ChatRoomDTO::new)
                .collect(Collectors.toList());
    }

    // Assign staff to room
    public ChatRoomDTO assignStaffToRoom(String roomId, User staff) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        room.setStaff(staff);
        room.setStatus(ChatRoom.RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        // Notify customer that staff joined
        String joinMessage = String.format("%s đã tham gia hỗ trợ bạn",
                staff.getFullName() != null ? staff.getFullName() : staff.getUsername());
        sendSystemMessage(roomId, joinMessage);

        log.info("Staff {} assigned to room {}", staff.getUsername(), roomId);

        return new ChatRoomDTO(room);
    }

    // Close chat room
    public void closeChatRoom(String roomId, User user) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        if (!hasRoomAccess(room, user)) {
            throw new RuntimeException("Access denied to close chat room: " + roomId);
        }

        room.setStatus(ChatRoom.RoomStatus.CLOSED);
        room.setClosedAt(LocalDateTime.now());
        roomRepository.save(room);

        // Send system message
        sendSystemMessage(roomId, "Cuộc trò chuyện đã được đóng");

        log.info("Chat room {} closed by {}", roomId, user.getUsername());
    }

    // Mark messages as read
    private void markMessagesAsRead(List<ChatMessage> messages, User user) {
        List<ChatMessage> unreadMessages = messages.stream()
                .filter(msg -> !msg.getIsRead() &&
                        msg.getReceiver() != null &&
                        msg.getReceiver().getId().equals(user.getId()))
                .collect(Collectors.toList());

        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(msg -> msg.setIsRead(true));
            messageRepository.saveAll(unreadMessages);
        }
    }

    // Send system message
    private void sendSystemMessage(String roomId, String content) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setSender(room.getCustomer()); // Use customer as sender for system messages
        systemMessage.setContent(content);
        systemMessage.setMessageType(ChatMessage.MessageType.SYSTEM);
        systemMessage.setTicketId(roomId);
        systemMessage.setIsRead(true); // System messages are auto-read

        systemMessage = messageRepository.save(systemMessage);

        ChatMessageDTO messageDTO = new ChatMessageDTO(systemMessage);
        messageDTO.setRoomId(roomId);

        messagingService.sendMessageToRoom(roomId, messageDTO);
    }

    // Check room access
    private boolean hasRoomAccess(ChatRoom room, User user) {
        return room.getCustomer().getId().equals(user.getId()) ||
                (room.getStaff() != null && room.getStaff().getId().equals(user.getId())) ||
                user.getRole() == User.UserRole.ADMIN ||
                user.getRole() == User.UserRole.EMPLOYEE;
    }

    // Generate unique room ID
    private String generateRoomId() {
        return "room_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Get unread message count for user
    public Long getUnreadMessageCount(User user) {
        return messageRepository.countUnreadMessagesForUser(user);
    }
}
