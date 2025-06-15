package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "NGUOIDUNG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenDangNhap", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "matKhau", nullable = false)
    private String password;

    @Column(name = "hoTen", nullable = false)
    private String fullName;

    @Column(name = "soDienThoai")
    private String phone;

    @Column(name = "diaChi")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "vaiTro", nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Column(name = "trangThai", nullable = false)
    private Boolean active = true;

    @Column(name = "daXacThucEmail", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    public enum UserRole {
        ADMIN, EMPLOYEE, CUSTOMER
    }
}