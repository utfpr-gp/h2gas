package com.h2gas.controller;


import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.services.ProductItemService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-items")
public class ProductItemController {
    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private ProductItemMapper productItemMapper;

    @Autowired
    private CreateProductItemMapper createProductItemMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ProductItemDTO>> getAllProductsItems() {
        List<ProductItemDTO> productItemDTO = productItemService.findAllProductItems();
        return ResponseEntity.ok(productItemDTO);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ProductItemDTO> createCategoryProduct(@RequestBody CreateProductItemDTO productItemDTO) {
        ProductItemDTO productItemCreate = productItemService.createProductItem(productItemDTO);
        return ResponseEntity.ok(productItemCreate);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ProductItemDTO> getProductById(@PathVariable Long id) {
        ProductItemDTO productItem = productItemService.findById(id);
        return ResponseEntity.ok(productItem);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Long> deleteProduct(@PathVariable("id") Long id) {
        productItemService.deleteProductItem(id);
        return ResponseEntity.ok(id);
    }

//    @RolesAllowed({"ADMIN", "SUPPLIER"})
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ProductItemDTO> update(@PathVariable Long id, @RequestBody CreateProductItemDTO dto) {
        ProductItemDTO updated = productItemService.updateProductItem(id, dto);
        return ResponseEntity.ok(updated);
    }

//    @GetMapping("/by-supplier/{supplierId}")
//    @ResponseBody
//    public ResponseEntity<SupplierDTO> getAddressByUserId(@PathVariable String userId) {
//        SupplierDTO supplierDTO = supplierService.findByUserIdSupplier(userId);
//        return ResponseEntity.ok(supplierDTO);
//    }

}
