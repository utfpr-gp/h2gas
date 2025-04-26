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

//    TODO IMPLEMENTAR ISSO QUANDO TERMINAR O SUPPLIER
//    @JsonManagedReference
//    @OneToMany(mappedBy = "city")
//    private List<Supplier> suppliers = new ArrayList<>();
}
