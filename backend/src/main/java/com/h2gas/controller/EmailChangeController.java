package com.h2gas.controller;

import com.h2gas.model.dto.EmailChangeConfirmDto;
import com.h2gas.model.dto.EmailChangeRequestDto;
import com.h2gas.model.dto.LoginDTO;
import com.h2gas.model.entity.User;
import com.h2gas.services.EmailChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários")
@RestController
@RequestMapping("/api/users/me/email-change")
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class EmailChangeController {

    private final EmailChangeService emailChangeService;

    public EmailChangeController(EmailChangeService emailChangeService) {
        this.emailChangeService = emailChangeService;
    }

    @Operation(summary = "Pede a troca do próprio e-mail (admin)",
            description = "Envia um código de 6 dígitos, válido por 15 minutos, para o e-mail novo. O e-mail atual só muda "
                    + "depois da confirmação. Um pedido novo substitui o anterior")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Código gerado e enviado ao e-mail novo"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido, igual ao atual ou já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Papel diferente de ADMIN")
    })
    @PostMapping
    public ResponseEntity<Void> requestEmailChange(@Parameter(hidden = true) @AuthenticationPrincipal User actor,
                                                   @RequestBody @Valid EmailChangeRequestDto data) {
        emailChangeService.requestChange(actor, data.newEmail());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirma a troca do próprio e-mail (admin)",
            description = "Com o código certo, troca o e-mail e devolve um JWT novo (o token antigo deixa de valer, "
                    + "porque o e-mail identifica o usuário no token). Até 5 tentativas por código")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-mail trocado; corpo com o token novo"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou esgotado, ou e-mail ocupado no meio-tempo"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Papel diferente de ADMIN")
    })
    @PostMapping("/confirm")
    public ResponseEntity<LoginDTO> confirmEmailChange(@Parameter(hidden = true) @AuthenticationPrincipal User actor,
                                                       @RequestBody @Valid EmailChangeConfirmDto data) {
        return ResponseEntity.ok(new LoginDTO(emailChangeService.confirmChange(actor, data.code())));
    }
}
