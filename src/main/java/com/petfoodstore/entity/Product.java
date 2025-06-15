package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SANPHAM")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenSanPham", nullable = false)
    private String name;

    @Column(name = "moTa", columnDefinition = "TEXT")
    private String description;

    @Column(name = "gia", nullable = false)
    private BigDecimal price;

    @Column(name = "soLuong", nullable = false)
    private Integer quantity = 0;

    @Column(name = "danhMuc")
    private String category;

    @Column(name = "thuongHieu")
    private String brand;

    @Column(name = "hinhAnh")
    private String imageUrl;

    @Column(name = "kichThuoc")
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiThuCung")
    private PetType petType;

    @Column(name = "trangThai", nullable = false)
    private Boolean active = true;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ngayCapNhat")
    private LocalDateTime updatedAt;

    public enum PetType {
        DOG, CAT, BIRD, FISH, RABBIT, OTHER
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}