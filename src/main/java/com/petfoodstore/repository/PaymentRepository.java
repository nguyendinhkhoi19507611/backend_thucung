package com.petfoodstore.repository;

import com.petfoodstore.entity.Payment;
import com.petfoodstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByMomoOrderId(String momoOrderId);

    Optional<Payment> findByMomoRequestId(String momoRequestId);

    List<Payment> findByOrder(Order order);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    List<Payment> findByMethod(Payment.PaymentMethod method);

    List<Payment> findByOrderAndMethod(Order order, Payment.PaymentMethod method);

    Optional<Payment> findByOrderAndStatus(Order order, Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.method = :method ORDER BY p.createdAt DESC")
    List<Payment> findByStatusAndMethod(@Param("status") Payment.PaymentStatus status,
                                        @Param("method") Payment.PaymentMethod method);

    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC")
    List<Payment> findAllOrderByCreatedAtDesc();

    // Find latest payment for an order
    @Query("SELECT p FROM Payment p WHERE p.order = :order ORDER BY p.createdAt DESC LIMIT 1")
    Optional<Payment> findLatestByOrder(@Param("order") Order order);

    // Count payments by status
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") Payment.PaymentStatus status);

    // Count payments by method
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.method = :method")
    Long countByMethod(@Param("method") Payment.PaymentMethod method);
}