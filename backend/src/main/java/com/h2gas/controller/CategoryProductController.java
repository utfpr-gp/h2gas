package com.h2gas.controller;

import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.services.CategoryProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categorias de Produto")
@RestController
@RequestMapping("/api/category-products")
public class CategoryProductController {
    @Autowired
    private CategoryProductService categoryProductService;

    @Autowired
    private CategoryProductMapper categoryProductMapper;

    @Operation(summary = "Lista todas as categorias de produto (pública)",
            description = "Aberta a visitantes. As categorias são globais — cadastradas pelo admin, por tipo de produto — e "
                    + "servem ao formulário de produto do fornecedor. Ordenadas por tipo e nome")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Todas as categorias, com o tipo"))
    @SecurityRequirements
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<CategoryProductDTO>> getAllCategoriesProducts() {
        return ResponseEntity.ok(categoryProductService.findAllCategoryProducts());
    }

    @Operation(summary = "Lista as categorias que a loja do fornecedor usa",
            description = "As categorias dos produtos da loja do token, ordenadas por tipo e nome, com o ícone do tipo e "
                    + "a quantidade de produtos da loja em cada uma")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias usadas pela loja"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)")
    })
    @GetMapping("/mine")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<List<SupplierCategoryDTO>> findMyCategories() {
        return ResponseEntity.ok(categoryProductService.findMine(currentUserId()));
    }

    @Operation(summary = "Cadastra uma categoria de produto",
            description = "Só o admin cadastra, sempre ligada a um tipo de produto. O nome não pode se repetir no mesmo tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria cadastrada"),
            @ApiResponse(responseCode = "400", description = "Nome vazio ou com mais de 60 caracteres, tipo ausente ou \"Categoria já cadastrada.\""),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de admin"),
            @ApiResponse(responseCode = "404", description = "Tipo de produto inexistente")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<CategoryProductDTO> createCategoryProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Nome e tipo da categoria")
            @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryProductService.createCategory(request));
    }

    @Operation(summary = "Remove uma categoria de produto",
            description = "Só o admin remove, e só categoria sem produtos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria removida; devolve o id"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de admin"),
            @ApiResponse(responseCode = "404", description = "Categoria inexistente"),
            @ApiResponse(responseCode = "409", description = "Categoria com produtos")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Long> deleteCategory(@Parameter(description = "ID da categoria") @PathVariable("id") Long id) {
        categoryProductService.deleteCategory(id);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Edita uma categoria de produto", description = "Só o admin edita; o nome não pode se repetir no mesmo tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "400", description = "Nome vazio ou com mais de 60 caracteres, tipo ausente ou \"Categoria já cadastrada.\""),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de admin"),
            @ApiResponse(responseCode = "404", description = "Categoria ou tipo de produto inexistente")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<CategoryProductDTO> update(@Parameter(description = "ID da categoria") @PathVariable Long id,
                                                     @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.ok(categoryProductService.update(id, request));
    }

    private static String currentUserId() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
