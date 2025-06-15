package com.petfoodstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRevenueDTO {
    private Long productId;
    private String productName;
    private BigDecimal totalRevenue;
    private Integer quantitySold;
} 