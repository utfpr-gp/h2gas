package com.h2gas.controller;

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

    @Operation(summary = "Cadastra uma categoria do fornecedor",
            description = "Só o fornecedor cria categorias, sempre ligadas a um tipo de produto mantido pelo admin")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria cadastrada"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)"),
            @ApiResponse(responseCode = "404", description = "Tipo de produto ou fornecedor inexistente")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> createCategoryProduct(@RequestBody CategoryProductDTO categoryProductDTO) {
        CategoryProductDTO categoryProductCreate = categoryProductService.createCategory(categoryProductDTO);
        return ResponseEntity.ok(categoryProductCreate);
    }

    @Operation(summary = "Remove uma categoria do fornecedor", description = "Só o fornecedor remove categorias")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria removida; devolve o id"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)"),
            @ApiResponse(responseCode = "404", description = "Categoria inexistente")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Long> deleteCategory(@Parameter(description = "ID da categoria") @PathVariable("id") Long id) {
        categoryProductService.deleteCategory(id);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Edita uma categoria do fornecedor", description = "Só o fornecedor edita categorias")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de fornecedor (admin incluso)"),
            @ApiResponse(responseCode = "404", description = "Categoria ou tipo de produto inexistente")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> update(@Parameter(description = "ID da categoria") @PathVariable Long id, @RequestBody CategoryProductDTO dto) {
        CategoryProductDTO updated = categoryProductService.update(id, dto);
        return ResponseEntity.ok(updated);
    }
}
