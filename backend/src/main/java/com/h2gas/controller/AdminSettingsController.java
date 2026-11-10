package com.h2gas.controller;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AppSettingDTO;
import com.h2gas.model.dto.AppSettingsDTO;
import com.h2gas.model.dto.UpdateAppSettingDTO;
import com.h2gas.services.AppSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

// HU26: o admin liga e desliga as mensagens ao celular e escolhe WhatsApp ou SMS
@Tag(name = "Configurações", description = "Configuração do sistema pelo administrador")
@RestController
@RequestMapping("/api/admin/settings")
@RolesAllowed("ADMIN")
public class AdminSettingsController {

    private final AppSettingService appSettingService;

    public AdminSettingsController(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @Operation(summary = "Lista os interruptores do sistema (admin)",
            description = "Mensagens ao celular pelo Twilio (código de login e aviso de saiu para entrega), o canal (WhatsApp ou SMS) "
                    + "e se cada canal está configurado no ambiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuração atual"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou não é admin")
    })
    @GetMapping
    public ResponseEntity<AppSettingsDTO> list() {
        return ResponseEntity.ok(appSettingService.list());
    }

    @Operation(summary = "Liga ou desliga um interruptor (admin)",
            description = "Vale na hora. Ligar uma mensagem, ou trocar de canal, sem o canal configurado é recusado; desligar sempre pode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuração gravada"),
            @ApiResponse(responseCode = "400", description = "Corpo sem 'enabled' ou canal não configurado"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou não é admin"),
            @ApiResponse(responseCode = "404", description = "Configuração inexistente")
    })
    @PutMapping("/{key}")
    public ResponseEntity<AppSettingDTO> update(
            @Parameter(description = "Configuração", example = "PHONE_LOGIN_CODE") @PathVariable String key,
            @Valid @RequestBody UpdateAppSettingDTO request) {
        AppSettingKey settingKey = Arrays.stream(AppSettingKey.values()).filter(k -> k.name().equals(key)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Configuração inexistente: " + key));
        return ResponseEntity.ok(appSettingService.update(settingKey, request.enabled()));
    }
}
