package com.h2gas.model.entity;

import com.h2gas.enums.DeliveryModeEnum;
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
    @JoinColumn(name = "address_id")
    private Address deliveryAddress;

    private LocalDateTime orderDate;

    private BigDecimal totalPrice;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private DeliveryModeEnum deliveryMode = DeliveryModeEnum.DELIVERY;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    // HU07: recusas de entrega, da mais recente para a mais antiga (só leitura)
    @OneToMany(mappedBy = "order")
    @OrderBy("createdAt DESC")
    private List<DeliveryRefusal> refusals;

    private String note;

    private LocalDateTime deliveryDate;

    @ManyToOne
    @JoinColumn(name = "deliverer_id")
    private Deliverer deliverer;

    @Column(precision = 10, scale = 2)
    private BigDecimal changeFor;

    private LocalDateTime dispatchedAt;

    private LocalDateTime deliveredAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
