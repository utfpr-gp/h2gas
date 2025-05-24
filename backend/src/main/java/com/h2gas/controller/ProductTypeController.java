package com.h2gas.controller;

import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.services.ProductTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-types")
public class ProductTypeController {
    @Autowired
    private ProductTypeService productTypeService;

    @Autowired
    private ProductTypeMapper productTypeMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<ProductTypeDTO>> getAllTypesProducts() {
        List<ProductTypeDTO> productTypeDTO = productTypeService.findAllProductTypes();
        return ResponseEntity.ok(productTypeDTO);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ProductTypeDTO> createTypesProducts(@RequestBody ProductTypeDTO productTypeDTO) {
        ProductTypeDTO productTypeCreate = productTypeService.createProductType(productTypeDTO);
        return ResponseEntity.ok(productTypeCreate);
    }

}
