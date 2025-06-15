package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "THANHTOAN")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donHangId", nullable = false)
    private Order order;

    @Column(name = "maThanhToan", unique = true, nullable = false)
    private String paymentId;

    @Column(name = "maGiaoDich", nullable = false)
    private String transactionId;

    @Column(name = "soTien", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuongThuc", nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "maDonHangMomo")
    private String momoOrderId;

    @Column(name = "maYeuCauMomo")
    private String momoRequestId;

    @Column(name = "chuKyMomo")
    private String momoSignature;

    @Column(name = "maTraLoi")
    private String responseCode;

    @Column(name = "thongDiepTraLoi")
    private String responseMessage;

    @Column(name = "urlThanhToan")
    private String paymentUrl;

    @Column(name = "duLieuTraLoi", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ngayThanhToan")
    private LocalDateTime paidAt;

    @Column(name = "ngayCapNhat")
    private LocalDateTime updatedAt;

    public enum PaymentMethod {
        CASH_ON_DELIVERY, // Thanh toán khi nhận hàng
        MOMO,            // Thanh toán MoMo
        BANK_TRANSFER    // Chuyển khoản ngân hàng
    }

    public enum PaymentStatus {
        PENDING,    // Chờ thanh toán
        PROCESSING, // Đang xử lý
        COMPLETED,  // Đã thanh toán thành công
        FAILED,     // Thanh toán thất bại
        CANCELLED,  // Đã hủy
        REFUNDED    // Đã hoàn tiền
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}