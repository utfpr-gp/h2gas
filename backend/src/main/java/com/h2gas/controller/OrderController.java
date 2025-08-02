package com.h2gas.controller;

import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.OrderService;
import com.h2gas.util.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PageResponse<OrderResponseDTO>> listAllOrderItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<OrderResponseDTO> result = orderService.findAllOrders(pageable);
        PageResponse<OrderResponseDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO orderResponseDTO = orderService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(orderResponseDTO);
    }


}
