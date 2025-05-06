package com.h2gas.model.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String streetName;

    private String number;

    private String complement;

    private String neighborhood;

    private String zipCode;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

}
