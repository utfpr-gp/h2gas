package com.h2gas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_reviews_order", columnNames = "order_id"))
@Check(name = "chk_reviews_rating", constraints = "rating BETWEEN 1 AND 5")
@Check(name = "chk_reviews_deliverer_rating", constraints = "deliverer_rating BETWEEN 1 AND 5")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "deliverer_id")
    private Deliverer deliverer;

    // nota da loja
    private Integer rating;

    // HU24: nota do entregador; nula na retirada ou quando o cliente não deu
    @Column(name = "deliverer_rating")
    private Integer delivererRating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
