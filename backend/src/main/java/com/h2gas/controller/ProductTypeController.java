package com.h2gas.controller;

import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.services.ProductTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tipos de Produto")
@RestController
@RequestMapping("/api/product-types")
public class ProductTypeController {
    @Autowired
    private ProductTypeService productTypeService;

    @Autowired
    private ProductTypeMapper productTypeMapper;

    @Operation(summary = "Lista os tipos de produto (categorias)",
            description = "Leitura pública de nomes de categoria (ex.: Gás, Água), sem dado sensível — usada pelo cliente para montar os filtros do catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tipos de produto")
    })
    @GetMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<List<ProductTypeDTO>> getAllTypesProducts() {
        List<ProductTypeDTO> productTypeDTO = productTypeService.findAllProductTypes();
        return ResponseEntity.ok(productTypeDTO);
    }

    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductTypeDTO> createTypesProducts(@RequestBody ProductTypeDTO productTypeDTO) {
        ProductTypeDTO productTypeCreate = productTypeService.createProductType(productTypeDTO);
        return ResponseEntity.ok(productTypeCreate);
    }

}
