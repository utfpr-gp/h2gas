package com.h2gas.controller;

import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.services.SupplierService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<List<SupplierDTO>> getAllSuppliers() {
        List<SupplierDTO> supplierDTO = supplierService.findAllSuppliers();
        return ResponseEntity.ok(supplierDTO);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody SupplierDTO supplierDTO) {
        SupplierDTO supplierCreate = supplierService.createSupplier(supplierDTO);
        return ResponseEntity.ok(supplierCreate);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Long> deleteSupplier(@PathVariable("id") Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(id);
    }

    @PatchMapping("/{id}/update-active")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateSupplierStatus(@PathVariable Long id,
                                                     @RequestBody Map<String, Boolean> status) {
        boolean novoStatus = status.get("active");
        supplierService.updateStatusSupplier(id, novoStatus);
        return ResponseEntity.ok(Map.of("message", "Status atualizado com sucesso"));
    }

}
