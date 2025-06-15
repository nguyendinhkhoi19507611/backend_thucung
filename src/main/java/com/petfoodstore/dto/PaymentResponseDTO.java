package com.petfoodstore.dto;

import com.petfoodstore.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String paymentId;
    private String transactionId;
    private BigDecimal amount;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private String paymentUrl;
    private String qrCodeUrl;
    private String deeplink;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}