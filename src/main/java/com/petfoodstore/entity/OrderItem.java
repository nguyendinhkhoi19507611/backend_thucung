package com.petfoodstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "CHITIETDONHANG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donHangId", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "sanPhamId", nullable = false)
    private Product product;

    @Column(name = "soLuong", nullable = false)
    private Integer quantity;

    @Column(name = "gia", nullable = false)
    private BigDecimal price;

    // Constructor for creating order item
    public OrderItem(Order order, Product product, Integer quantity) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice();
    }
}