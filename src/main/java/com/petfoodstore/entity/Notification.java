package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "THONGBAO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nguoiDungId", nullable = false)
    private User user;

    @Column(name = "tieuDe", nullable = false)
    private String title;

    @Column(name = "noiDung", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiThongBao", nullable = false)
    private NotificationType type;

    @Column(name = "daDoc", nullable = false)
    private Boolean isRead = false;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ngayDoc")
    private LocalDateTime readAt;

    // Reference to related entities
    @ManyToOne
    @JoinColumn(name = "donHangId")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "sanPhamId")
    private Product product;

    // Action URL for clickable notifications
    @Column(name = "urlHanhDong")
    private String actionUrl;

    // Additional data as JSON string
    @Column(name = "duLieuBoSung", columnDefinition = "TEXT")
    private String metadata;

    public enum NotificationType {
        ORDER_CREATED,          // New order created
        ORDER_STATUS_UPDATED,   // Order status changed
        PAYMENT_SUCCESSFUL,     // Payment completed
        PAYMENT_FAILED,         // Payment failed
        NEW_MESSAGE,            // New chat message
        PRODUCT_LOW_STOCK,      // Product low stock (admin)
        WELCOME,                // Welcome message for new users
        PROMOTION,              // Promotional notifications
        SYSTEM                  // System notifications
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}