package com.h2gas.controller;

import com.h2gas.model.dto.PushPublicKeyDTO;
import com.h2gas.model.dto.PushSubscriptionRequestDTO;
import com.h2gas.model.dto.PushUnsubscribeDTO;
import com.h2gas.model.entity.User;
import com.h2gas.services.PushSubscriptionService;
import com.h2gas.services.WebPushSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// HU27: o navegador do usuário logado assina (ou deixa de assinar) as notificações push
@Tag(name = "Notificações", description = "Notificações push do PWA")
@RestController
@RequestMapping("/api/push")
@RolesAllowed({"CUSTOMER", "SUPPLIER", "DELIVERER", "ADMIN"})
public class PushSubscriptionController {

    private final PushSubscriptionService service;
    private final WebPushSender sender;

    public PushSubscriptionController(PushSubscriptionService service, WebPushSender sender) {
        this.service = service;
        this.sender = sender;
    }

    @Operation(summary = "Chave pública VAPID", description = "O navegador usa esta chave para assinar o push deste servidor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave e se o push está configurado"),
            @ApiResponse(responseCode = "401", description = "Sem token válido")
    })
    @GetMapping("/public-key")
    public ResponseEntity<PushPublicKeyDTO> publicKey() {
        return ResponseEntity.ok(new PushPublicKeyDTO(sender.publicKey(), sender.isConfigured()));
    }

    @Operation(summary = "Grava a assinatura de push deste aparelho",
            description = "O dono é o usuário do token. Se o endpoint já existia com outro usuário, ele passa para o atual")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assinatura gravada"),
            @ApiResponse(responseCode = "400", description = "Endpoint ou chaves ausentes"),
            @ApiResponse(responseCode = "401", description = "Sem token válido")
    })
    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequestDTO request) {
        service.subscribe(loggedUser().getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apaga a assinatura de push deste aparelho", description = "Só apaga se a assinatura for do usuário do token")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assinatura apagada (ou não existia)"),
            @ApiResponse(responseCode = "400", description = "Endpoint ausente"),
            @ApiResponse(responseCode = "401", description = "Sem token válido")
    })
    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeDTO request) {
        service.unsubscribe(loggedUser().getId(), request.endpoint());
        return ResponseEntity.noContent().build();
    }

    private User loggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
