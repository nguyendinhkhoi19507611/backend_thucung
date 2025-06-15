package com.petfoodstore.dto;

import com.petfoodstore.entity.ChatRoom;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private String roomId;
    private Long customerId;
    private String customerName;
    private Long staffId;
    private String staffName;
    private String roomType;
    private String status;
    private String priority;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime closedAt;
    private Integer unreadCount;
    private ChatMessageDTO lastMessage;

    // Constructor from entity
    public ChatRoomDTO(ChatRoom room) {
        this.id = room.getId();
        this.roomId = room.getRoomId();
        this.customerId = room.getCustomer().getId();
        this.customerName = room.getCustomer().getFullName() != null ?
                room.getCustomer().getFullName() : room.getCustomer().getUsername();

        if (room.getStaff() != null) {
            this.staffId = room.getStaff().getId();
            this.staffName = room.getStaff().getFullName() != null ?
                    room.getStaff().getFullName() : room.getStaff().getUsername();
        }

        this.roomType = room.getRoomType().name();
        this.status = room.getStatus().name();
        this.priority = room.getPriority().name();
        this.subject = room.getSubject();
        this.createdAt = room.getCreatedAt();
        this.lastMessageAt = room.getLastMessageAt();
        this.closedAt = room.getClosedAt();
    }
}