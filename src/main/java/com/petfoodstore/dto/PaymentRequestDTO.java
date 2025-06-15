package com.petfoodstore.dto;

import com.petfoodstore.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    private String extraData;

    @NotBlank(message = "Return URL is required")
    private String returnUrl;
}