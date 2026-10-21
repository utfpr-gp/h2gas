package com.h2gas.model.dto;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDTO {
    private String id;
    private String name;
    private String phone;
    private String description;
    // Novo formato
    private String openingTime;  // ex: "08:00"
    private String closingTime;  // ex: "18:00"

    private Long averageDeliveryTime;
    private boolean active;
    private boolean open;
    private String cityName;
    private Long cityId;
    private String email;
    private String cnpj;
    private String logoUrl;
    @Schema(description = "Banner da loja no Cloudinary; nulo sem banner (a página da loja usa o padrão)", nullable = true)
    private String bannerUrl;
    @Schema(description = "Média das avaliações da loja, com 1 casa decimal; nula sem avaliações. Preenchida só no catálogo do cliente", example = "4.3", nullable = true)
    private Double averageRating;
    @Schema(description = "Quantidade de avaliações da loja; 0 sem avaliações. Preenchida só no catálogo do cliente", example = "14")
    private Long reviewCount = 0L;
    private List<ProductItemDTO> products;
//    private UserDto user;

    //    address relacionamento
    private String addressNeighborhood;
    private String addressComplement;
    private String addressStreetName;
    private String addressNumber;
    private Long addressId;
}
