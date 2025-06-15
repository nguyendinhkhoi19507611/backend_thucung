package com.petfoodstore.dto;

import lombok.Data;

@Data
public class MoMoPaymentResponse {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private Long amount;
    private Long responseTime;
    private String message;
    private String resultCode;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
}