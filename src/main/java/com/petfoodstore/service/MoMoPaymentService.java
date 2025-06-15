package com.petfoodstore.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.petfoodstore.config.MoMoConfig;
import com.petfoodstore.dto.MoMoPaymentRequest;
import com.petfoodstore.dto.MoMoPaymentResponse;
import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.Payment;
import com.petfoodstore.repository.OrderRepository;
import com.petfoodstore.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MoMoPaymentService {

    @Autowired
    private MoMoConfig moMoConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;


    private final Gson gson = new Gson();

    public MoMoPaymentResponse createPayment(Order order, String extraData) {
        try {
            log.info("Creating MoMo payment for order: {}", order.getOrderNumber());

            // Kiểm tra nếu đã có payment MoMo cho order này
            List<Payment> existingMoMoPayments = paymentRepository.findByOrderAndMethod(order, Payment.PaymentMethod.MOMO);
            if (!existingMoMoPayments.isEmpty()) {
                Payment existingPayment = existingMoMoPayments.get(0);
                if (existingPayment.getStatus() == Payment.PaymentStatus.PENDING ||
                        existingPayment.getStatus() == Payment.PaymentStatus.PROCESSING) {
                    log.info("Using existing MoMo payment for order: {}", order.getOrderNumber());
                    return convertPaymentToMoMoResponse(existingPayment);
                }
            }

            if (!moMoConfig.isEnabled()) {
                log.warn("MoMo payment is not enabled, using test service");
                throw new RuntimeException("MoMo payment is not enabled");
            }

            // Tạo transaction ID trước
            String transactionId = UUID.randomUUID().toString();

            // Tạo request tới MoMo
            MoMoPaymentRequest request = buildMoMoRequest(order, transactionId, extraData);
            log.info("MoMo Request: {}",request);
            // Gửi request tới MoMo
            String responseJson = sendHttpRequest(request);
            log.info("MoMo Response: {}", responseJson);

            // Parse response
            JsonObject responseObj = gson.fromJson(responseJson, JsonObject.class);
            MoMoPaymentResponse response = new MoMoPaymentResponse();

            response.setPartnerCode(getJsonString(responseObj, "partnerCode"));
            response.setOrderId(getJsonString(responseObj, "orderId"));
            response.setRequestId(getJsonString(responseObj, "requestId"));
            response.setAmount(getJsonLong(responseObj, "amount"));
            response.setResponseTime(getJsonLong(responseObj, "responseTime"));
            response.setMessage(getJsonString(responseObj, "message"));
            response.setResultCode(getJsonString(responseObj, "resultCode"));
            response.setPayUrl(getJsonString(responseObj, "payUrl"));
            response.setDeeplink(getJsonString(responseObj, "deeplink"));
            response.setQrCodeUrl(getJsonString(responseObj, "qrCodeUrl"));

            // Tạo Payment record sau khi có response từ MoMo
            Payment payment = createPaymentRecord(order, transactionId, response, responseJson);

            // Set orderId cho response để PaymentService có thể tìm được
            response.setOrderId(transactionId);

            log.info("Created MoMo payment successfully for order: {}", order.getOrderNumber());
            return response;

        } catch (Exception e) {
            log.error("Error creating MoMo payment for order: " + order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create MoMo payment: " + e.getMessage());
        }
    }

    private MoMoPaymentResponse convertPaymentToMoMoResponse(Payment payment) {
        MoMoPaymentResponse response = new MoMoPaymentResponse();
        response.setPartnerCode(moMoConfig.getPartnerCode());
        response.setOrderId(payment.getTransactionId());
        response.setRequestId(payment.getMomoRequestId());
        response.setAmount(payment.getAmount().longValue());
        response.setResponseTime(System.currentTimeMillis());
        response.setMessage(payment.getResponseMessage());
        response.setResultCode(payment.getResponseCode());
        response.setPayUrl(payment.getPaymentUrl());

        // Parse từ raw response nếu có
        if (payment.getRawResponse() != null) {
            try {
                JsonObject rawObj = gson.fromJson(payment.getRawResponse(), JsonObject.class);
                response.setDeeplink(getJsonString(rawObj, "deeplink"));
                response.setQrCodeUrl(getJsonString(rawObj, "qrCodeUrl"));
            } catch (Exception e) {
                log.warn("Failed to parse raw response for existing payment");
            }
        }

        return response;
    }

    private Payment createPaymentRecord(Order order, String transactionId, MoMoPaymentResponse response, String rawResponse) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(Payment.PaymentMethod.MOMO);
        payment.setTransactionId(transactionId);

        // Set payment_id từ response hoặc tạo một ID unique
        payment.setPaymentId(response.getRequestId() != null ? response.getRequestId() : "MOMO_" + System.currentTimeMillis());
        payment.setMomoOrderId(response.getOrderId());
        payment.setMomoRequestId(response.getRequestId());
        payment.setResponseCode(response.getResultCode());
        payment.setResponseMessage(response.getMessage());
        payment.setPaymentUrl(response.getPayUrl());
        payment.setRawResponse(rawResponse);

        if ("0".equals(response.getResultCode())) {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            log.info("MoMo payment created with PROCESSING status");
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            log.warn("MoMo payment created with FAILED status. Result code: {}", response.getResultCode());
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Saved payment record with ID: {} for transaction: {}", savedPayment.getId(), transactionId);
        return savedPayment;
    }

    private MoMoPaymentRequest buildMoMoRequest(Order order, String transactionId, String extraData) {
        String orderId = transactionId;
        String requestId = UUID.randomUUID().toString();
        long amount = order.getTotalAmount().longValue();
        String orderInfo = "Thanh toán đơn hàng " + order.getOrderNumber();

        if (extraData == null || extraData.trim().isEmpty()) {
            extraData = "";
        }

        // Tạo raw signature
        String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                moMoConfig.getAccessKey(),
                amount,
                extraData,
                moMoConfig.getNotifyUrl(),
                orderId,
                orderInfo,
                moMoConfig.getPartnerCode(),
                moMoConfig.getReturnUrl(),
                requestId,
                moMoConfig.getRequestType()
        );

        log.debug("MoMo signature string: {}", rawSignature);
        String signature = generateSignature(rawSignature, moMoConfig.getSecretKey());

        // Build request data
        ;
        MoMoPaymentRequest request = new MoMoPaymentRequest(
                moMoConfig.getPartnerCode(),
                requestId,
                amount,
                orderId,
                orderInfo,
                moMoConfig.getReturnUrl(),
                moMoConfig.getNotifyUrl(),
                "vi",
                15,
                extraData,
                moMoConfig.getRequestType(),
                signature,
                true
        );
//        MoMoPaymentRequest request = new MoMoPaymentRequest();
//        request.setPartnerCode(moMoConfig.getPartnerCode());
//        request.setPartnerName("PetFoodStore");
//        request.setStoreId("PetFoodStore");
//        request.setRequestType(moMoConfig.getRequestType());
//        request.setIpnUrl(moMoConfig.getNotifyUrl());
//        request.setRedirectUrl(moMoConfig.getReturnUrl());
//        request.setOrderId(orderId);
//        request.setAmount(amount);
//        request.setLang("vi");
//        request.setOrderInfo(orderInfo);
//        request.setRequestId(requestId);
//        request.setExtraData(extraData);
//        request.setSignature(signature);

        return request;
    }

    private String sendHttpRequest(MoMoPaymentRequest request) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(moMoConfig.getEndpoint());
            httpPost.setHeader("Content-Type", "application/json");

            String jsonRequest = gson.toJson(request);
            log.info("MoMo Request: {}", jsonRequest);

            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    log.error("MoMo API returned status code: {}, response: {}", statusCode, responseBody);
                    throw new RuntimeException("MoMo API error: " + statusCode);
                }

                return responseBody;
            }
        }
    }

    public boolean verifyPayment(String orderId, String requestId, String amount,
                                 String orderInfo, String orderType, String transId,
                                 String resultCode, String message, String localMessage,
                                 String responseTime, String extraData, String signature) {

        try {
            // Tạo raw signature để verify
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    moMoConfig.getAccessKey(),
                    amount,
                    extraData != null ? extraData : "",
                    message,
                    orderId,
                    orderInfo,
                    orderType != null ? orderType : "",
                    moMoConfig.getPartnerCode(), requestId,
                    responseTime,
                    resultCode,
                    transId != null ? transId : ""
            );

            log.debug("Verify signature string: {}", rawSignature);
            String expectedSignature = generateSignature(rawSignature, moMoConfig.getSecretKey());
            boolean isValid = expectedSignature.equals(signature);

            log.info("Signature verification result: {} for orderId: {}", isValid, orderId);
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying MoMo signature", e);
            return false;
        }
    }

    public void handlePaymentCallback(String orderId, String requestId, String resultCode,
                                      String message, String transId) {
        try {
            log.info("Processing MoMo callback for orderId: {}, resultCode: {}", orderId, resultCode);

            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + orderId));

            payment.setMomoRequestId(requestId);
            payment.setResponseCode(resultCode);
            payment.setResponseMessage(message);
            payment.setUpdatedAt(LocalDateTime.now());

            if ("0".equals(resultCode)) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());

                // Cập nhật trạng thái đơn hàng
                Order order = payment.getOrder();
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                    log.info("Updated order status to CONFIRMED for order: {}", order.getOrderNumber());
                }
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("Payment failed for orderId: {}, resultCode: {}, message: {}", orderId, resultCode, message);
            }

            paymentRepository.save(payment);
            log.info("Payment callback processed successfully for orderId: {}", orderId);

        } catch (Exception e) {
            log.error("Error processing payment callback for orderId: " + orderId, e);
            throw new RuntimeException("Failed to process payment callback: " + e.getMessage());
        }
    }

    public PaymentStatus checkPaymentStatus(String transactionId) {
        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionId));

            // TODO: Implement actual MoMo status check API call
            // For now, just return current status
            return new PaymentStatus(payment.getStatus(), payment.getResponseMessage());

        } catch (Exception e) {
            log.error("Error checking payment status for transaction: " + transactionId, e);
            throw new RuntimeException("Failed to check payment status: " + e.getMessage());
        }
    }

    private String generateSignature(String rawSignature, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature", e);
            throw new RuntimeException("Error generating signature", e);
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private Long getJsonLong(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : null;
    }

    // Inner class for payment status response
    public static class PaymentStatus {
        private Payment.PaymentStatus status;
        private String message;

        public PaymentStatus(Payment.PaymentStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public Payment.PaymentStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}