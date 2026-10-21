package com.h2gas.controller;

import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.CategoryProductService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    @ResponseBody
    @RolesAllowed({"ADMIN"})
    public ResponseEntity<List<CategoryProductDTO>> getAllCategoriesProducts() {
        List<CategoryProductDTO> categoryProductDTO = categoryProductService.findAllCategoryProducts();
        return ResponseEntity.ok(categoryProductDTO);
    }

    @GetMapping("/by-supplier/{supplierId}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<PageResponse<CategoryProductDTO>> findAllCategoriesBySupplier(
            @PathVariable String supplierId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<CategoryProductDTO> categoryProductDTO = categoryProductService.findAllBySupplierId(supplierId, pageable);
        PageResponse<CategoryProductDTO> response = new PageResponse<>(categoryProductDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lista as categorias do fornecedor logado",
            description = "Todas as categorias da loja do token, ordenadas por tipo e nome, com o ícone do tipo e a quantidade de produtos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias da loja"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)")
    })
    @GetMapping("/mine")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<List<SupplierCategoryDTO>> findMyCategories() {
        return ResponseEntity.ok(categoryProductService.findMine(currentUserId()));
    }

    @Operation(summary = "Cadastra uma categoria do fornecedor",
            description = "Só o fornecedor cria categorias, sempre ligadas a um tipo de produto mantido pelo admin. "
                    + "A loja vem do token; o nome não pode se repetir no mesmo tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria cadastrada"),
            @ApiResponse(responseCode = "400", description = "Nome vazio ou com mais de 60 caracteres, ou tipo ausente"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)"),
            @ApiResponse(responseCode = "404", description = "Tipo de produto inexistente"),
            @ApiResponse(responseCode = "409", description = "Categoria com o mesmo nome no mesmo tipo")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> createCategoryProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Nome e tipo da categoria")
            @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryProductService.createCategory(currentUserId(), request));
    }

    @Operation(summary = "Remove uma categoria do fornecedor",
            description = "Só o dono remove, e só categoria sem produtos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria removida; devolve o id"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel diferente de fornecedor ou categoria de outra loja"),
            @ApiResponse(responseCode = "404", description = "Categoria inexistente"),
            @ApiResponse(responseCode = "409", description = "Categoria com produtos")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Long> deleteCategory(@Parameter(description = "ID da categoria") @PathVariable("id") Long id) {
        categoryProductService.deleteCategory(id, currentUserId());
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Edita uma categoria do fornecedor", description = "Só o dono edita; o nome não pode se repetir no mesmo tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "400", description = "Nome vazio ou com mais de 60 caracteres, ou tipo ausente"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel diferente de fornecedor ou categoria de outra loja"),
            @ApiResponse(responseCode = "404", description = "Categoria ou tipo de produto inexistente"),
            @ApiResponse(responseCode = "409", description = "Categoria com o mesmo nome no mesmo tipo")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> update(@Parameter(description = "ID da categoria") @PathVariable Long id,
                                                     @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.ok(categoryProductService.update(id, request, currentUserId()));
    }

    private static String currentUserId() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
