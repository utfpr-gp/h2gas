package com.h2gas.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "suppliers")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier extends User{
//    @Id
//    @GeneratedValue(strategy= GenerationType.IDENTITY)
//    private Long id;
//
//    private String name;
//
//    private String phone;
    private String cnpj;

    private String description;

//    private String workingHours;
    private LocalTime openingTime;
    private LocalTime closingTime;

//    private String averageDeliveryTime;
    private Duration averageDeliveryTime;

    private boolean active;


    @OneToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

//    @OneToOne
//    @JoinColumn(name = "user_id")
//    private User user;

    @OneToMany(mappedBy = "supplier")
    private List<ProductItem> productItems;


    // abaixo disso o produto aparece como estoque baixo na tela de produtos do fornecedor
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 8;

    // logo da loja no Cloudinary; nula = o catálogo mostra o ícone de loja
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // F7 §5.26: banner da loja no Cloudinary; nulo = a página da loja mostra o banner padrão
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;


}
