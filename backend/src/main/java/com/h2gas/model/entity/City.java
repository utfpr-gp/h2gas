package com.h2gas.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String state;

    // código de município do IBGE; nulo só nas cidades cadastradas antes da F7 fatia 6
    @Column(name = "ibge_code", unique = true)
    private Integer ibgeCode;

    public City(Long id, String name, String state) {
        this.id = id;
        this.name = name;
        this.state = state;
    }
}
