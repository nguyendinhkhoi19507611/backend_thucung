package com.petfoodstore.dto;

import com.petfoodstore.entity.ChatMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private String content;
    private String messageType;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private String roomId;
    private Long orderId;
    private String orderNumber;
    private String ticketId;

    // Constructor from entity
    public ChatMessageDTO(ChatMessage message) {
        this.id = message.getId();
        this.content = message.getContent();
        this.messageType = message.getMessageType().name();
        this.senderId = message.getSender().getId();
        this.senderName = message.getSender().getFullName() != null ?
                message.getSender().getFullName() : message.getSender().getUsername();
        this.senderRole = message.getSender().getRole().name();

        if (message.getReceiver() != null) {
            this.receiverId = message.getReceiver().getId();
            this.receiverName = message.getReceiver().getFullName() != null ?
                    message.getReceiver().getFullName() : message.getReceiver().getUsername();
        }

        this.createdAt = message.getCreatedAt();
        this.isRead = message.getIsRead();
        this.ticketId = message.getTicketId();

        if (message.getOrder() != null) {
            this.orderId = message.getOrder().getId();
            this.orderNumber = message.getOrder().getOrderNumber();
        }
    }
}