package com.h2gas.controller;


import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.services.ProductItemService;
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

//    @GetMapping("/by-supplier/{supplierId}")
//    @ResponseBody
//    public ResponseEntity<SupplierDTO> getAddressByUserId(@PathVariable String userId) {
//        SupplierDTO supplierDTO = supplierService.findByUserIdSupplier(userId);
//        return ResponseEntity.ok(supplierDTO);
//    }

}
