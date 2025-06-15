package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DONHANG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maDonHang", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "nguoiDungId", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "tongTien", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuongThucThanhToan")
    private PaymentMethod paymentMethod;

    @Column(name = "diaChiGiaoHang")
    private String shippingAddress;

    @Column(name = "soDienThoai")
    private String phone;

    @Column(name = "ghiChu")
    private String notes;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ngayCapNhat")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    public enum PaymentMethod {
        CASH_ON_DELIVERY, // Thanh toán khi nhận hàng
        MOMO,            // Thanh toán MoMo
        BANK_TRANSFER    // Chuyển khoản ngân hàng
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to calculate total
    public void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}