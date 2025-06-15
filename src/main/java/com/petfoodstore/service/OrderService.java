package com.petfoodstore.service;

import com.petfoodstore.dto.NotificationDTO;
import com.petfoodstore.dto.OrderDTO;
import com.petfoodstore.dto.ProductRevenueDTO;
import com.petfoodstore.entity.*;
import com.petfoodstore.repository.OrderRepository;
import com.petfoodstore.repository.ProductRepository;
import com.petfoodstore.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OrderService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketMessagingService messagingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public Order createOrder(OrderDTO orderDTO, User user) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setShippingAddress(orderDTO.getShippingAddress());
        order.setPhone(orderDTO.getPhone());
        order.setNotes(orderDTO.getNotes());
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setStatus(Order.OrderStatus.PENDING);

        // Add order items
        for (OrderDTO.OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Check stock
            if (product.getQuantity() < itemDTO.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            // Create order item
            OrderItem orderItem = new OrderItem(order, product, itemDTO.getQuantity());
            order.getOrderItems().add(orderItem);

            // Update product stock
            product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
            productRepository.save(product);

            // Check low stock after order
            checkLowStockAfterOrder(product);
        }

        // Calculate total
        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);

        // Send notifications
        notificationService.createOrderNotification(savedOrder, Notification.NotificationType.ORDER_CREATED);

        // Send real-time update to admin/employee
        NotificationDTO adminNotification = new NotificationDTO();
        adminNotification.setTitle("Đơn hàng mới");
        adminNotification.setMessage(String.format("Đơn hàng #%s từ %s cần xử lý",
                savedOrder.getOrderNumber(),
                user.getFullName() != null ? user.getFullName() : user.getUsername()));
        adminNotification.setType("ORDER_CREATED");
        adminNotification.setIsRead(false);
        adminNotification.setCreatedAt(LocalDateTime.now());
        adminNotification.setActionUrl("/employee/orders");
        adminNotification.setOrderId(savedOrder.getId());
        adminNotification.setOrderNumber(savedOrder.getOrderNumber());

        messagingService.sendAdminNotification(adminNotification);

        log.info("New order created: {} for user: {}", savedOrder.getOrderNumber(), user.getUsername());

        return savedOrder;
    }

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = getOrderById(id);
        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        // Automatically update payment status for cash payments when order is delivered
        if (status == Order.OrderStatus.DELIVERED && order.getPaymentMethod() == Order.PaymentMethod.CASH_ON_DELIVERY) {
            List<Payment> payments = paymentRepository.findByOrderAndMethod(order, Payment.PaymentMethod.CASH_ON_DELIVERY);
            if (!payments.isEmpty()) {
                Payment payment = payments.get(0);
                if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setResponseMessage("Payment completed upon delivery");
                    paymentRepository.save(payment);
                    
                    // Send payment success notification
                    notificationService.createPaymentNotification(order, Notification.NotificationType.PAYMENT_SUCCESSFUL);
                    log.info("Updated payment status to COMPLETED for COD order: {}", order.getOrderNumber());
                }
            }
        }

        // Send notification to customer
        notificationService.createOrderNotification(updatedOrder, Notification.NotificationType.ORDER_STATUS_UPDATED);

        // Send real-time update to customer
        messagingService.sendOrderUpdate(order.getUser().getId(), updatedOrder);

        log.info("Order {} status updated from {} to {}", order.getOrderNumber(), oldStatus, status);

        return updatedOrder;
    }

    public void cancelOrder(Long id) {
        Order order = getOrderById(id);

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Can only cancel pending orders");
        }

        // Restore product stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order {} cancelled", order.getOrderNumber());
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // Check for low stock and send notification
    private void checkLowStockAfterOrder(Product product) {
        if (product.getQuantity() <= 5 && product.getQuantity() > 0) {
            notificationService.createLowStockNotification(product);
            log.warn("Low stock warning for product: {} (quantity: {})", product.getName(), product.getQuantity());
        }
    }

    public List<ProductRevenueDTO> getProductRevenueByMonth(YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusNot(
            startDate, endDate, Order.OrderStatus.CANCELLED);

        Map<Product, ProductRevenueDTO> revenueMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                ProductRevenueDTO revenue = revenueMap.computeIfAbsent(product,
                    p -> new ProductRevenueDTO(p.getId(), p.getName(), BigDecimal.ZERO, 0));
                
                revenue.setTotalRevenue(revenue.getTotalRevenue()
                    .add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                revenue.setQuantitySold(revenue.getQuantitySold() + item.getQuantity());
            }
        }

        return new ArrayList<>(revenueMap.values());
    }

    public List<ProductRevenueDTO> getProductRevenueByQuarter(int year, int quarter) {
        YearMonth startMonth = YearMonth.of(year, (quarter - 1) * 3 + 1);
        YearMonth endMonth = YearMonth.of(year, quarter * 3);
        
        LocalDateTime startDate = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = endMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusNot(
            startDate, endDate, Order.OrderStatus.CANCELLED);

        Map<Product, ProductRevenueDTO> revenueMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                ProductRevenueDTO revenue = revenueMap.computeIfAbsent(product,
                    p -> new ProductRevenueDTO(p.getId(), p.getName(), BigDecimal.ZERO, 0));
                
                revenue.setTotalRevenue(revenue.getTotalRevenue()
                    .add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                revenue.setQuantitySold(revenue.getQuantitySold() + item.getQuantity());
            }
        }

        return new ArrayList<>(revenueMap.values());
    }

    public List<ProductRevenueDTO> getProductRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusNot(
            startDate, endDate, Order.OrderStatus.CANCELLED);

        Map<Product, ProductRevenueDTO> revenueMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                ProductRevenueDTO revenue = revenueMap.computeIfAbsent(product,
                    p -> new ProductRevenueDTO(p.getId(), p.getName(), BigDecimal.ZERO, 0));
                
                revenue.setTotalRevenue(revenue.getTotalRevenue()
                    .add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                revenue.setQuantitySold(revenue.getQuantitySold() + item.getQuantity());
            }
        }

        return new ArrayList<>(revenueMap.values());
    }
}