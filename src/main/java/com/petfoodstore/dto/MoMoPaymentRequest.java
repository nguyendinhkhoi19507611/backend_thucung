package com.petfoodstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MoMoPaymentRequest {
    @JsonProperty("partnerCode")
    String partnerCode;

    @JsonProperty("requestId")
    String requestId;

    @JsonProperty("amount")
    long amount;

    @JsonProperty("orderId")
    String orderId;

    @JsonProperty("orderInfo")
    String orderInfo;

    @JsonProperty("redirectUrl")
    String redirectUrl;

    @JsonProperty("ipnUrl")
    String ipnUrl;

    @JsonProperty("lang")
    String lang;

    @JsonProperty("orderExpireTime")
    int orderExpireTime;

    @JsonProperty("extraData")
    String extraData;

    @JsonProperty("requestType")
    String requestType;

    @JsonProperty("signature")
    String signature;

    @JsonProperty("autoCapture")
    boolean autoCapture;
}