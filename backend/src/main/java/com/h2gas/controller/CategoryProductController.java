package com.h2gas.controller;

import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.CategoryProductService;
import com.h2gas.util.PaginationUtil;
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

    @PostMapping
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> createCategoryProduct(@RequestBody CategoryProductDTO categoryProductDTO) {
        CategoryProductDTO categoryProductCreate = categoryProductService.createCategory(categoryProductDTO);
        return ResponseEntity.ok(categoryProductCreate);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Long> deleteCategory(@PathVariable("id") Long id) {
        categoryProductService.deleteCategory(id);
        return ResponseEntity.ok(id);
    }

    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<CategoryProductDTO> update(@PathVariable Long id, @RequestBody CategoryProductDTO dto) {
        CategoryProductDTO updated = categoryProductService.update(id, dto);
        return ResponseEntity.ok(updated);
    }
}
