package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "MAXACTHUC")
@Data
@NoArgsConstructor
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maXacThuc", nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguoiDungId", nullable = false)
    private User user;

    @Column(name = "ngayHetHan", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "daSuDung", nullable = false)
    private boolean used = false;

    public EmailVerificationToken(User user, String token, int expirationMinutes) {
        this.user = user;
        this.token = token;
        this.expiryDate = LocalDateTime.now().plusMinutes(expirationMinutes);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
} 