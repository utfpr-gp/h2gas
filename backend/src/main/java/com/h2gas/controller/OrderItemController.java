package com.h2gas.controller;

import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.mapper.OrderItemMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.OrderItemService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Itens de Pedido")
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {
    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @GetMapping
    @ResponseBody
    @RolesAllowed({"ADMIN"})
    public ResponseEntity<PageResponse<OrderItemResponseDTO>> listAllOrderItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<OrderItemResponseDTO> result = orderItemService.findAllOrdersItems(pageable);
        PageResponse<OrderItemResponseDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN"})
    public ResponseEntity<OrderItemResponseDTO> getProductById(@PathVariable Long id) {
        OrderItemResponseDTO orderItemResponseDTO = orderItemService.findByIdOrderItem(id);
        return ResponseEntity.ok(orderItemResponseDTO);
    }
}
