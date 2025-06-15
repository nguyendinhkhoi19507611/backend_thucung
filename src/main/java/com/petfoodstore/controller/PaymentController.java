package com.petfoodstore.controller;

import com.petfoodstore.dto.*;
import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.Payment;
import com.petfoodstore.entity.User;
import com.petfoodstore.repository.PaymentRepository;
import com.petfoodstore.service.MoMoPaymentService;
import com.petfoodstore.service.OrderService;
import com.petfoodstore.service.PaymentService;
import com.petfoodstore.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MoMoPaymentService moMoPaymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPayment(@Valid @RequestBody PaymentRequestDTO request,
                                           Authentication authentication) {
        try {
            log.info("Creating payment for order: {} with method: {}", request.getOrderId(), request.getPaymentMethod());

            User user = userService.findByUsername(authentication.getName());
            Order order = orderService.getOrderById(request.getOrderId());

            // Kiểm tra quyền sở hữu đơn hàng
            if (!order.getUser().getId().equals(user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("EMPLOYEE"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Bạn không có quyền thanh toán đơn hàng này"));
            }

            PaymentResponseDTO response;

            switch (request.getPaymentMethod()) {
                case MOMO:
                    log.info("Processing MoMo payment for order: {}", order.getOrderNumber());
                    MoMoPaymentResponse momoResponse = moMoPaymentService.createPayment(order, request.getExtraData());
                    response = paymentService.convertMoMoResponseToDTO(momoResponse, order);
                    break;
                case CASH_ON_DELIVERY:
                    log.info("Processing COD payment for order: {}", order.getOrderNumber());
                    response = paymentService.createCashOnDeliveryPayment(order);
                    break;
                case BANK_TRANSFER:
                    log.info("Processing Bank Transfer payment for order: {}", order.getOrderNumber());
                    response = paymentService.createBankTransferPayment(order);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Phương thức thanh toán không được hỗ trợ"));
            }

            log.info("Payment created successfully with ID: {}", response.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating payment for order: " + request.getOrderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi tạo thanh toán: " + e.getMessage()));
        }
    }

    @PostMapping("/momo/callback")
    public ResponseEntity<?> handleMoMoCallback(@RequestParam Map<String, String> params) {
        try {
            log.info("MoMo callback received: {}", params);

            String partnerCode = params.get("partnerCode");
            String orderId = params.get("orderId");
            String requestId = params.get("requestId");
            String amount = params.get("amount");
            String orderInfo = params.get("orderInfo");
            String orderType = params.get("orderType");
            String transId = params.get("transId");
            String resultCode = params.get("resultCode");
            String message = params.get("message");
            String localMessage = params.get("localMessage");
            String responseTime = params.get("responseTime");
            String extraData = params.get("extraData");
            String signature = params.get("signature");

            // Verify signature
            boolean isValid = moMoPaymentService.verifyPayment(
                    orderId, requestId, amount, orderInfo, orderType,
                    transId, resultCode, message, localMessage,
                    responseTime, extraData, signature
            );

            if (!isValid) {
                log.warn("Invalid MoMo signature for orderId: {}", orderId);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid signature"));
            }

            // Process payment
            moMoPaymentService.handlePaymentCallback(orderId, requestId, resultCode, message, transId);

            return ResponseEntity.ok(new MessageResponse("Callback processed successfully"));

        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing callback"));
        }
    }

    @GetMapping("/return")
    public ResponseEntity<?> handlePaymentReturn(@RequestParam Map<String, String> params) {
        try {
            log.info("Payment return received: {}", params);

            String orderId = params.get("orderId");
            String resultCode = params.get("resultCode");
            String message = params.get("message");

            PaymentResponseDTO payment = paymentService.getPaymentByTransactionId(orderId);

            if (payment == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Payment not found"));
            }

            // Update payment status if needed
            if ("0".equals(resultCode) && payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                paymentService.updatePaymentStatus(payment.getId(), Payment.PaymentStatus.COMPLETED);
                payment = paymentService.getPaymentById(payment.getId()); // Refresh data
            }

            return ResponseEntity.ok(payment);

        } catch (Exception e) {
            log.error("Error handling payment return", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing return"));
        }
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPaymentsByOrder(@PathVariable Long orderId,
                                                Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Order order = orderService.getOrderById(orderId);

            // Kiểm tra quyền truy cập
            if (!order.getUser().getId().equals(user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("EMPLOYEE"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Bạn không có quyền xem thanh toán này"));
            }

            List<PaymentResponseDTO> payments = paymentService.getPaymentsByOrderId(orderId);
            return ResponseEntity.ok(payments);

        } catch (Exception e) {
            log.error("Error getting payments for order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi lấy thông tin thanh toán"));
        }
    }

    @GetMapping("/status/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String transactionId,
                                                Authentication authentication) {
        try {
            log.info("Checking payment status for transaction: {}", transactionId);

            PaymentResponseDTO payment = paymentService.getPaymentByTransactionId(transactionId);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            User user = userService.findByUsername(authentication.getName());
            Order order = orderService.getOrderById(payment.getOrderId());

            // Kiểm tra quyền truy cập
            if (!order.getUser().getId().equals(user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("EMPLOYEE"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Bạn không có quyền xem thanh toán này"));
            }

            // Refresh payment status if it's still pending/processing
            if (payment.getStatus() == Payment.PaymentStatus.PENDING ||
                    payment.getStatus() == Payment.PaymentStatus.PROCESSING) {
                // For MoMo payments, we could implement status check with MoMo API here
                // For now, just return current status
            }

            return ResponseEntity.ok(payment);

        } catch (Exception e) {
            log.error("Error checking payment status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi kiểm tra trạng thái thanh toán"));
        }
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPaymentDetails(@PathVariable Long paymentId,
                                               Authentication authentication) {
        try {
            PaymentResponseDTO payment = paymentService.getPaymentById(paymentId);

            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            User user = userService.findByUsername(authentication.getName());
            Order order = orderService.getOrderById(payment.getOrderId());

            // Kiểm tra quyền truy cập
            if (!order.getUser().getId().equals(user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("EMPLOYEE"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Bạn không có quyền xem thanh toán này"));
            }

            return ResponseEntity.ok(payment);

        } catch (Exception e) {
            log.error("Error getting payment details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi lấy chi tiết thanh toán"));
        }
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long paymentId,
                                                 @RequestParam Payment.PaymentStatus status) {
        try {
            PaymentResponseDTO updatedPayment = paymentService.updatePaymentStatus(paymentId, status);
            return ResponseEntity.ok(updatedPayment);

        } catch (Exception e) {
            log.error("Error updating payment status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi cập nhật trạng thái thanh toán"));
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> getAllPayments(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) Payment.PaymentStatus status,
                                            @RequestParam(required = false) Payment.PaymentMethod method) {
        try {
            List<PaymentResponseDTO> payments = paymentService.getAllPayments(page, size, status, method);
            return ResponseEntity.ok(payments);

        } catch (Exception e) {
            log.error("Error getting all payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi lấy danh sách thanh toán"));
        }
    }

    @PostMapping("/test/complete/{transactionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> simulatePaymentComplete(@PathVariable String transactionId) {
        try {
            log.info("Simulating payment completion for transaction: {}", transactionId);

            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with transaction ID: " + transactionId));

            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setResponseMessage("Test payment completed successfully");

            // Cập nhật trạng thái đơn hàng
            Order order = payment.getOrder();
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
            }

            paymentRepository.save(payment);
            log.info("Payment simulation completed successfully for transaction: {}", transactionId);

            return ResponseEntity.ok(new MessageResponse("Payment simulation completed successfully"));

        } catch (Exception e) {
            log.error("Error simulating payment completion for transaction: " + transactionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId,
                                           @RequestBody(required = false) Map<String, String> refundData) {
        try {
            PaymentResponseDTO refundedPayment = paymentService.refundPayment(paymentId, refundData);
            return ResponseEntity.ok(refundedPayment);

        } catch (Exception e) {
            log.error("Error refunding payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi hoàn tiền: " + e.getMessage()));
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyPayments(Authentication authentication,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<PaymentResponseDTO> payments = paymentService.getPaymentsByUserId(user.getId(), page, size);
            return ResponseEntity.ok(payments);

        } catch (Exception e) {
            log.error("Error getting user payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Lỗi khi lấy lịch sử thanh toán"));
        }
    }
}