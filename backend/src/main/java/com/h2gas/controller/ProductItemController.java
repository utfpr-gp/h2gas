package com.h2gas.controller;


import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.ProductItemService;
import com.h2gas.util.PaginationUtil;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductItemDTO> createCategoryProduct(
            @RequestPart("product") CreateProductItemDTO productItemDTO,
            @RequestPart("file") MultipartFile file
    ) {
        ProductItemDTO productItemCreate = productItemService.createProductItem(productItemDTO, file);
        return ResponseEntity.ok(productItemCreate);
    }
//    public ResponseEntity<ProductItemDTO> createCategoryProduct(
//            @RequestBody CreateProductItemDTO productItemDTO,
//            @RequestParam("file") MultipartFile file
//    ) {
//        ProductItemDTO productItemCreate = productItemService.createProductItem(productItemDTO, file);
//        return ResponseEntity.ok(productItemCreate);
//    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ProductItemDTO> getProductById(@PathVariable Long id) {
        ProductItemDTO productItem = productItemService.findById(id);
        return ResponseEntity.ok(productItem);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Long> deleteProduct(@PathVariable("id") Long id) {
        productItemService.deleteProductItem(id);
        return ResponseEntity.ok(id);
    }

    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<ProductItemDTO> update(@PathVariable Long id, @RequestBody CreateProductItemDTO dto) {
        ProductItemDTO updated = productItemService.updateProductItem(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/by-supplier/{supplierId}")
    @ResponseBody
    public ResponseEntity<PageResponse<ProductItemDTO>> getProductBySupplier(
            @PathVariable String supplierId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<ProductItemDTO> result = productItemService.findProductsBySupplier(supplierId, pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-supplier-open")
    @ResponseBody
    public ResponseEntity<PageResponse<ProductItemDTO>> getAllProductsBySupplierOpen(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<ProductItemDTO> result = productItemService.findBySupplierActiveAndSupplierOpen(pageable);
        PageResponse<ProductItemDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

}
