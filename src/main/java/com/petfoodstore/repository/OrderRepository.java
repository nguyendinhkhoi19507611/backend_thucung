package com.petfoodstore.repository;

import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByCreatedAtBetweenAndStatusNot(
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Order.OrderStatus excludeStatus
    );
}