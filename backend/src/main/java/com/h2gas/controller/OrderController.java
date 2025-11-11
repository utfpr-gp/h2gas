package com.h2gas.controller;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.OrderService;
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
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<OrderResponseDTO> getProductById(@PathVariable String id) {
        OrderResponseDTO orderResponseDTO = orderService.findByIdOrder(id);
        return ResponseEntity.ok(orderResponseDTO);
    }

    @GetMapping
    @ResponseBody
    @RolesAllowed({"ADMIN"})
    public ResponseEntity<PageResponse<OrderResponseDTO>> listAllOrderItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(required = false) OrderStatusEnum status
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<OrderResponseDTO> result = null;

        if (status != null) {
            result = orderService.findByStatus(status,pageable);
        } else {
            result = orderService.findAllOrders(pageable);
        }

        PageResponse<OrderResponseDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO orderResponseDTO = orderService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(orderResponseDTO);
    }

    @GetMapping("/by-customer/{customerId}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public ResponseEntity<PageResponse<OrderResponseDTO>> listAllOrderItems(
            @PathVariable String customerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<OrderResponseDTO> result = orderService.findByCustomerId(customerId, status, pageable);
        PageResponse<OrderResponseDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{supplierId}/exclude-status")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<PageResponse<OrderResponseDTO>> lisAllOrderItemExcludeStatus(
            @PathVariable String supplierId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "excludeStatuses", required = false) List<OrderStatusEnum> excludeStatuses
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<OrderResponseDTO> result = orderService.findBySupplierIdAndStatusNotDeliveryAndCanceled(supplierId, pageable, excludeStatuses);
        PageResponse<OrderResponseDTO> response = new PageResponse<>(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-supplier/{supplierId}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<PageResponse<OrderResponseDTO>> listAllOrdersBySupplier(
            @PathVariable String supplierId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(required = false) OrderStatusEnum status
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);

        Page<OrderResponseDTO> result = null;

        if (status != null) {
            result = orderService.findBySupplierIdAndStatus(supplierId, status, pageable);
        } else {
            result = orderService.findBySupplierId(supplierId, pageable);
        }

        PageResponse<OrderResponseDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/update-order-status")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Map<String, String>> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> status) {
        String novoStatus = orderService.updateOrderStatus(id, status.get("orderStatus"));
        return ResponseEntity.ok(Map.of("status", novoStatus));
    }
}
