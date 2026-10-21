package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product_types")
@Getter
@Setter
@NoArgsConstructor
public class ProductType {
    public static final String DEFAULT_ICON = "category";

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, length = 40)
    private String icon = DEFAULT_ICON;

    @OneToMany(mappedBy = "productType", cascade = CascadeType.ALL)
    private Set<CategoryProduct> categories = new HashSet<>();

    public ProductType(Long id, String name, Set<CategoryProduct> categories) {
        this.id = id;
        this.name = name;
        this.categories = categories;
    }

//    @ManyToOne
//    @JoinColumn(name = "supplier_id", nullable = false)
//    private Supplier supplier;
}
