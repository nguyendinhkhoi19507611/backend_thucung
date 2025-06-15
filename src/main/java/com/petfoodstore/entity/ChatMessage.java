package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TINNHAN")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nguoiGuiId", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "nguoiNhanId")
    private User receiver; // null for public messages

    @Column(name = "noiDung", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiTinNhan", nullable = false)
    private MessageType messageType = MessageType.CHAT;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "daDoc", nullable = false)
    private Boolean isRead = false;

    // Order ID for order-related messages
    @ManyToOne
    @JoinColumn(name = "donHangId")
    private Order order;

    // Support ticket ID if this is part of support conversation
    @Column(name = "maPhieuHoTro")
    private String ticketId;

    public enum MessageType {
        CHAT,           // Regular chat message
        ORDER_UPDATE,   // Order status update
        SUPPORT,        // Support message
        SYSTEM         // System message
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}