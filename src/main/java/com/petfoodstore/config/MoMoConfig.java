package com.petfoodstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "momo")
@Data
public class MoMoConfig {
    // Sử dụng thông tin test sandbox chính thức của MoMo
    private String partnerCode = "MOMO";
    private String accessKey = "F8BBA842ECF85";
    private String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String returnUrl = "http://localhost:3000/payment/return";
    private String notifyUrl = "http://localhost:8080/api/payments/momo/callback";
    private String requestType = "captureWallet"; // hoặc "captureWallet"
    private boolean enabled = true;
}