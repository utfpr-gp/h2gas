package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Double price;

    @ManyToOne
    @JoinColumn(name = "category_product_id")
    private CategoryProduct categoryProduct;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
}
