package com.h2gas.controller;

import com.h2gas.model.dto.AdminDashboardDto;
import com.h2gas.services.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Administração", description = "Gestão da plataforma pelo administrador")
@RestController
@RequestMapping("/api/admin/dashboard")
@RolesAllowed("ADMIN")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Painel da plataforma",
            description = "Usuários por perfil, fornecedores aprovados e aguardando aprovação, cidades, pedidos de hoje "
                    + "e do mês, pedidos por status e faturamento do mês (sem os cancelados). Só ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Números da plataforma"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN")
    })
    @GetMapping
    public ResponseEntity<AdminDashboardDto> dashboard() {
        return ResponseEntity.ok(dashboardService.buildDashboard(LocalDate.now()));
    }
}
