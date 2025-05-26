package com.h2gas.controller;

import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.services.CategoryProductService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-products")
public class CategoryProductController {
    @Autowired
    private CategoryProductService categoryProductService;

    @Autowired
    private CategoryProductMapper categoryProductMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<CategoryProductDTO>> getAllCategoriesProducts() {
        List<CategoryProductDTO> categoryProductDTO = categoryProductService.findAllCategoryProducts();
        return ResponseEntity.ok(categoryProductDTO);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<CategoryProductDTO> createCategoryProduct(@RequestBody CategoryProductDTO categoryProductDTO) {
        CategoryProductDTO categoryProductCreate = categoryProductService.createCategory(categoryProductDTO);
        return ResponseEntity.ok(categoryProductCreate);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Long> deleteCategory(@PathVariable("id") Long id) {
        categoryProductService.deleteCategory(id);
        return ResponseEntity.ok(id);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<CategoryProductDTO> update(@PathVariable Long id, @RequestBody CategoryProductDTO dto) {
        CategoryProductDTO updated = categoryProductService.update(id, dto);
        return ResponseEntity.ok(updated);
    }
}
