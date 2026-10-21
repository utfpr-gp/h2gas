package com.h2gas.controller;

import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.services.ProductTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Cadastra um tipo de produto",
            description = "Só ADMIN. O tipo fica disponível para todos os fornecedores criarem as próprias categorias. "
                    + "Nome obrigatório e único (sem diferenciar maiúsculas)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo criado"),
            @ApiResponse(responseCode = "400", description = "Nome vazio, longo demais ou já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<ProductTypeDTO> createTypesProducts(@Valid @RequestBody ProductTypeDTO productTypeDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productTypeService.createProductType(productTypeDTO));
    }

    @Operation(summary = "Renomeia um tipo de produto",
            description = "Só ADMIN. As categorias dos fornecedores continuam apontando para o mesmo tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo renomeado"),
            @ApiResponse(responseCode = "400", description = "Nome vazio, longo demais ou já usado por outro tipo"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN"),
            @ApiResponse(responseCode = "404", description = "Tipo não encontrado")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<ProductTypeDTO> updateTypesProducts(
            @Parameter(description = "Id do tipo") @PathVariable Long id,
            @Valid @RequestBody ProductTypeDTO productTypeDTO) {
        return ResponseEntity.ok(productTypeService.updateProductType(id, productTypeDTO));
    }

}
