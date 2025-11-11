package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductItem {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private BigDecimal price;
    private String description;
    private String imageUrl;
    private Integer stockQuantity;
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "category_product_id")
    private CategoryProduct categoryProduct;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
}
