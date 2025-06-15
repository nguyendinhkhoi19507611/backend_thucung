package com.petfoodstore.controller;

import com.petfoodstore.dto.OrderDTO;
import com.petfoodstore.dto.MessageResponse;
import com.petfoodstore.dto.ProductRevenueDTO;
import com.petfoodstore.entity.Order;
import com.petfoodstore.entity.User;
import com.petfoodstore.service.InvoiceService;
import com.petfoodstore.service.OrderService;
import com.petfoodstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private InvoiceService invoiceService;
    // Customer endpoints
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderDTO orderDTO,
                                             Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Order order = orderService.createOrder(orderDTO, user);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Order> orders = orderService.getOrdersByUser(user);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long id,
                                                 Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Order order = orderService.getOrderById(id);

        // Check if user owns this order or is admin/employee
        if (!order.getUser().getId().equals(user.getId()) &&
                !authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                a.getAuthority().equals("EMPLOYEE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    // Admin/Employee endpoints
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestParam Order.OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok(new MessageResponse("Order cancelled successfully!"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<?> cancelOrderByCustomer(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Order order = orderService.getOrderById(id);
        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        orderService.cancelOrder(id);
        return ResponseEntity.ok(new MessageResponse("Order cancelled successfully!"));
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<byte[]> exportInvoice(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice_" + order.getOrderNumber() + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // Revenue statistics endpoints
    @GetMapping("/revenue/monthly")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ProductRevenueDTO>> getMonthlyRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        List<ProductRevenueDTO> revenue = orderService.getProductRevenueByMonth(yearMonth);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/revenue/quarterly")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ProductRevenueDTO>> getQuarterlyRevenue(
            @RequestParam int year,
            @RequestParam int quarter) {
        if (quarter < 1 || quarter > 4) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductRevenueDTO> revenue = orderService.getProductRevenueByQuarter(year, quarter);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/revenue/custom")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ProductRevenueDTO>> getCustomRangeRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductRevenueDTO> revenue = orderService.getProductRevenueByDateRange(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }
}