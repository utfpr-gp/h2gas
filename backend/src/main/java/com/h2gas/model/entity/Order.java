package com.h2gas.model.entity;

import com.h2gas.enums.OrderStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @NonNull
    private User customer;

    @ManyToOne
    @NonNull
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    private Address deliveryAddress;

    private LocalDateTime orderDate;

    private BigDecimal totalPrice;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    private String note;

    private LocalDateTime deliveryDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
