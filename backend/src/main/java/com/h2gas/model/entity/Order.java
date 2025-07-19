package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private String id;

    @ManyToOne
    private User customer;

    @ManyToOne
    private User distributor;

    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    private Address deliveryAddress;

    private LocalDateTime orderDate;

    private BigDecimal totalPrice;

    private String paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    private String note;

    private LocalDateTime deliveryDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
