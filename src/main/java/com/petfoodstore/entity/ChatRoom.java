package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "PHONGCHAT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maPhong", unique = true, nullable = false)
    private String roomId;

    @ManyToOne
    @JoinColumn(name = "khachHangId", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "nhanVienId")
    private User staff; // Admin or Employee handling the chat

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiPhong", nullable = false)
    private RoomType roomType = RoomType.SUPPORT;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false)
    private RoomStatus status = RoomStatus.ACTIVE;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "tinNhanCuoiLuc")
    private LocalDateTime lastMessageAt;

    @Column(name = "ngayDong")
    private LocalDateTime closedAt;

    // Subject or title of the conversation
    @Column(name = "chuDe")
    private String subject;

    // Priority level
    @Enumerated(EnumType.STRING)
    @Column(name = "doUuTien")
    private Priority priority = Priority.NORMAL;

    public enum RoomType {
        SUPPORT,    // Customer support
        ORDER,      // Order-related chat
        GENERAL     // General inquiries
    }

    public enum RoomStatus {
        ACTIVE,     // Active conversation
        WAITING,    // Waiting for staff response
        CLOSED,     // Conversation closed
        RESOLVED    // Issue resolved
    }

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    public void updateLastMessageTime() {
        this.lastMessageAt = LocalDateTime.now();
    }
}