package com.petfoodstore.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.petfoodstore.dto.MoMoPaymentResponse;
import com.petfoodstore.dto.PaymentResponseDTO;
import com.petfoodstore.entity.Notification;
import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.Payment;
import com.petfoodstore.repository.OrderRepository;
import com.petfoodstore.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class PaymentService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketMessagingService messagingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final Gson gson = new Gson();

    public PaymentResponseDTO convertMoMoResponseToDTO(MoMoPaymentResponse momoResponse, Order order) {
        // Tìm payment record đã được tạo trong MoMoPaymentService
        Payment payment = paymentRepository.findByTransactionId(momoResponse.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment record not found for transaction: " + momoResponse.getOrderId()));

        return convertToDTO(payment);
    }

    public PaymentResponseDTO createCashOnDeliveryPayment(Order order) {
        // Kiểm tra nếu đã có payment COD cho order này
        List<Payment> existingPayments = paymentRepository.findByOrderAndMethod(order, Payment.PaymentMethod.CASH_ON_DELIVERY);
        if (!existingPayments.isEmpty()) {
            Payment existingPayment = existingPayments.get(0);
            log.info("Using existing COD payment for order: {}", order.getOrderNumber());
            return convertToDTO(existingPayment);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(Payment.PaymentMethod.CASH_ON_DELIVERY);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentId("COD_" + System.currentTimeMillis());
        payment.setResponseMessage("Thanh toán khi nhận hàng");

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created COD payment with ID: {} for order: {}", savedPayment.getId(), order.getOrderNumber());
        return convertToDTO(savedPayment);
    }

    public PaymentResponseDTO createBankTransferPayment(Order order) {
        // Kiểm tra nếu đã có payment Bank Transfer cho order này
        List<Payment> existingPayments = paymentRepository.findByOrderAndMethod(order, Payment.PaymentMethod.BANK_TRANSFER);
        if (!existingPayments.isEmpty()) {
            Payment existingPayment = existingPayments.get(0);
            log.info("Using existing Bank Transfer payment for order: {}", order.getOrderNumber());
            return convertToDTO(existingPayment);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(Payment.PaymentMethod.BANK_TRANSFER);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentId("BT_" + System.currentTimeMillis());
        payment.setResponseMessage("Chuyển khoản ngân hàng");

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created Bank Transfer payment with ID: {} for order: {}", savedPayment.getId(), order.getOrderNumber());
        return convertToDTO(savedPayment);
    }

    public PaymentResponseDTO getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public PaymentResponseDTO getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(this::convertToDTO)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<PaymentResponseDTO> getPaymentsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return paymentRepository.findAll()
                .stream()
                .filter(payment -> payment.getOrder().getUser().getId().equals(userId))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .skip((long) page * size)
                .limit(size)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PaymentResponseDTO updatePaymentStatus(Long paymentId, Payment.PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        Payment.PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());

        if (status == Payment.PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
            payment.setResponseMessage("Payment completed successfully");

            // Cập nhật trạng thái đơn hàng
            Order order = payment.getOrder();
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                log.info("Updated order status to CONFIRMED for order: {}", order.getOrderNumber());

                // Send order update notification
                notificationService.createOrderNotification(order, Notification.NotificationType.ORDER_STATUS_UPDATED);
                messagingService.sendOrderUpdate(order.getUser().getId(), order);
            }

            // Send payment success notification
            notificationService.createPaymentNotification(order, Notification.NotificationType.PAYMENT_SUCCESSFUL);

        } else if (status == Payment.PaymentStatus.FAILED) {
            payment.setResponseMessage("Payment failed");

            // Send payment failed notification
            notificationService.createPaymentNotification(payment.getOrder(), Notification.NotificationType.PAYMENT_FAILED);

        } else if (status == Payment.PaymentStatus.CANCELLED) {
            payment.setResponseMessage("Payment cancelled");
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Updated payment status from {} to {} for payment ID: {}", oldStatus, status, paymentId);

        return convertToDTO(savedPayment);
    }

    public List<PaymentResponseDTO> getAllPayments(int page, int size,
                                                   Payment.PaymentStatus status,
                                                   Payment.PaymentMethod method) {
        List<Payment> payments;

        // Apply filters
        if (status != null && method != null) {
            payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == status && p.getMethod() == method)
                    .collect(Collectors.toList());
        } else if (status != null) {
            payments = paymentRepository.findByStatus(status);
        } else if (method != null) {
            payments = paymentRepository.findByMethod(method);
        } else {
            payments = paymentRepository.findAll();
        }

        return payments.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .skip((long) page * size)
                .limit(size)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PaymentResponseDTO refundPayment(Long paymentId, Map<String, String> refundData) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Cannot refund payment that is not completed. Current status: " + payment.getStatus());
        }

        // TODO: Implement actual refund logic for different payment methods
        // For MoMo, you would need to call MoMo refund API
        // For now, just update the status

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setResponseMessage("Payment refunded successfully");

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Refunded payment ID: {} for order: {}", paymentId, payment.getOrder().getOrderNumber());
        return convertToDTO(savedPayment);
    }

    private PaymentResponseDTO convertToDTO(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder().getId());
        dto.setOrderNumber(payment.getOrder().getOrderNumber());
        dto.setPaymentId(payment.getPaymentId());
        dto.setTransactionId(payment.getTransactionId());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setPaymentUrl(payment.getPaymentUrl());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setPaidAt(payment.getPaidAt());

        // Thêm các trường cho MoMo
        if (payment.getMethod() == Payment.PaymentMethod.MOMO) {
            if (payment.getRawResponse() != null) {
                try {
                    JsonObject rawObj = gson.fromJson(payment.getRawResponse(), JsonObject.class);
                    dto.setQrCodeUrl(getJsonString(rawObj, "qrCodeUrl"));
                    dto.setDeeplink(getJsonString(rawObj, "deeplink"));
                } catch (Exception e) {
                    log.warn("Failed to parse raw response for payment ID: {}", payment.getId());
                }
            }

            // If no QR code URL from response, construct one
            if (dto.getQrCodeUrl() == null && payment.getPaymentUrl() != null) {
                dto.setQrCodeUrl(payment.getPaymentUrl());
            }
        }

        return dto;
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}