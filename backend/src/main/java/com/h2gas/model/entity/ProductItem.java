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
    // HU21: um preço só, sem frete embutido (o frete vem da distância, HU17)
    @Column(precision = 10, scale = 2, nullable = false)
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

    // F12: código aleatório usado na URL da edição, no lugar do id sequencial
    @Column(name = "public_id", nullable = false, unique = true, length = 12)
    private String publicId;

    private static final String PUBLIC_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            StringBuilder code = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                code.append(PUBLIC_ID_CHARS.charAt(RANDOM.nextInt(PUBLIC_ID_CHARS.length())));
            }
            publicId = code.toString();
        }
    }
}
