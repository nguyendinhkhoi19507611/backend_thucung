package com.petfoodstore.dto;

import com.petfoodstore.entity.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderDTO {
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String notes;

    @NotNull(message = "Payment method is required")
    private Order.PaymentMethod paymentMethod;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private Integer quantity;
    }
}