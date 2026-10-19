package com.h2gas.controller;

import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.DelivererDTO;
import com.h2gas.model.entity.User;
import com.h2gas.services.DelivererService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Entregadores")
@RestController
@RequestMapping("/api/deliverers")
public class DelivererController {

    private final DelivererService delivererService;

    public DelivererController(DelivererService delivererService) {
        this.delivererService = delivererService;
    }

    @Operation(summary = "Cadastra um entregador na frota do fornecedor logado",
            description = "O fornecedor é sempre o usuário autenticado. Cria o login do entregador (perfil DELIVERER) "
                    + "e o vincula à frota do fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entregador cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já cadastrado"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @PostMapping
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<DelivererDTO> create(@Valid @RequestBody DelivererCreateDTO dto) {
        return ResponseEntity.ok(delivererService.createDeliverer(dto, loggedUser().getId()));
    }

    @Operation(summary = "Lista a frota do fornecedor logado",
            description = "Cada entregador traz busy=true quando há pedido em rota (OUT_FOR_DELIVERY) com ele")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frota do fornecedor"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine")
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<List<DelivererDTO>> listMine() {
        return ResponseEntity.ok(delivererService.findMine(loggedUser().getId()));
    }

    private User loggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
