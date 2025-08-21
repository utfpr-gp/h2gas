package com.h2gas.controller;

import com.h2gas.model.dto.*;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.SupplierService;
import com.h2gas.util.PaginationUtil;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierMapper supplierMapper;

    @GetMapping
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<PageResponse<SupplierDTO>> getAllSuppliers(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.findAllSuppliers(pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<PageResponse<SupplierDTO>> searchSuppliers(
            @RequestParam String name,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.searchSuppliers(name, pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/actives/open")
    @ResponseBody
    public ResponseEntity<PageResponse<SupplierDTO>> getAllSuppliersOpenAndActive(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<List<SupplierSummaryDTO>> getAllSuppliersSummary() {
        List<SupplierSummaryDTO> supplierDTO = supplierService.findAllSuppliersSummary();
        return ResponseEntity.ok(supplierDTO);
    }

    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody SupplierCreateDTO supplierDTO) {
        SupplierDTO supplierCreate = supplierService.createSupplier(supplierDTO);
        return ResponseEntity.ok(supplierCreate);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Long> deleteSupplier(@PathVariable("id") Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(id);
    }

    @PatchMapping("/{id}/update-active")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Map<String, String>> updateSupplierStatus(@PathVariable Long id,
                                                     @RequestBody Map<String, Boolean> status) {
        boolean novoStatus = status.get("active");
        supplierService.updateStatusSupplier(id, novoStatus);
        return ResponseEntity.ok(Map.of("message", "Status atualizado com sucesso"));
    }

    @GetMapping("/by-user/{userId}")
    @ResponseBody
    public ResponseEntity<SupplierDTO> getAddressByUserId(@PathVariable String userId) {
        SupplierDTO supplierDTO = supplierService.findByUserIdSupplier(userId);
        return ResponseEntity.ok(supplierDTO);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<SupplierDTO> getAddressById(@PathVariable Long id) {
        SupplierDTO supplierDTO = supplierService.findById(id);
        return ResponseEntity.ok(supplierDTO);
    }

    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<SupplierDTO> update(@PathVariable Long id, @RequestBody SupplierCreateDTO dto) {
        SupplierDTO updated = supplierService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changeStatusOpenClosed(@PathVariable Long id,
                                                              @RequestBody Map<String, Boolean> status) {
        boolean novoStatus = status.get("status");
        supplierService.updateStatusOpenClosed(id, novoStatus);
        return ResponseEntity.ok(Map.of("message", "Status atualizado com sucesso"));
    }

}
