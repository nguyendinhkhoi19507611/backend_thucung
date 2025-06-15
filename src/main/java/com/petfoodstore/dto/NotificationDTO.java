package com.petfoodstore.dto;

import com.petfoodstore.entity.Notification;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String actionUrl;
    private String metadata;

    // Related entity info
    private Long orderId;
    private String orderNumber;
    private Long productId;
    private String productName;

    // Constructor from entity
    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.type = notification.getType().name();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
        this.actionUrl = notification.getActionUrl();
        this.metadata = notification.getMetadata();

        if (notification.getOrder() != null) {
            this.orderId = notification.getOrder().getId();
            this.orderNumber = notification.getOrder().getOrderNumber();
        }

        if (notification.getProduct() != null) {
            this.productId = notification.getProduct().getId();
            this.productName = notification.getProduct().getName();
        }
    }
}