package com.h2gas.model.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

//    private String productName;
    private int quantity;
    private BigDecimal unitPrice;

     @ManyToOne
     private ProductItem product;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
