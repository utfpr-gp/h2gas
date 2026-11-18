package com.h2gas.model.entity;


import com.h2gas.model.Coordinates;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String streetName;
    private String addressName;
    private String number;
    private String complement;

    private String neighborhood;

    private String zipCode;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // HU17: coordenadas do CEP (nulas quando a consulta falhou ou ainda não rodou)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    public Address(Long id, String streetName, String addressName, String number, String complement,
                   String neighborhood, String zipCode, City city, User user) {
        this.id = id;
        this.streetName = streetName;
        this.addressName = addressName;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.zipCode = zipCode;
        this.city = city;
        this.user = user;
    }

    public Coordinates coordinates() {
        return latitude == null || longitude == null ? null : new Coordinates(latitude.doubleValue(), longitude.doubleValue());
    }

    public void applyCoordinates(Coordinates coordinates) {
        this.latitude = coordinates == null ? null : BigDecimal.valueOf(coordinates.latitude()).setScale(7, RoundingMode.HALF_UP);
        this.longitude = coordinates == null ? null : BigDecimal.valueOf(coordinates.longitude()).setScale(7, RoundingMode.HALF_UP);
    }
}
