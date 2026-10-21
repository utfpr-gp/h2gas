package com.h2gas.controller;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AdminCreateUserDto;
import com.h2gas.model.dto.AdminSetEnabledDto;
import com.h2gas.model.dto.AdminUserDto;
import com.h2gas.model.entity.User;
import com.h2gas.response.PageResponse;
import com.h2gas.services.AdminUserService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administração", description = "Gestão da plataforma pelo administrador")
@RestController
@RequestMapping("/api/admin/users")
@RolesAllowed("ADMIN")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "Lista os usuários da plataforma",
            description = "Paginado e ordenado por nome. Filtra por perfil e por parte do nome ou do e-mail "
                    + "(sem diferenciar maiúsculas). Só ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN")
    })
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserDto>> list(
            @Parameter(description = "Perfil (vazio = todos)", example = "CUSTOMER")
            @RequestParam(value = "type", required = false) UserTypeEnum type,
            @Parameter(description = "Parte do nome ou do e-mail", example = "souza")
            @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Página, a partir de 0", example = "0")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Itens por página (máximo 50)", example = "10")
            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(new PageResponse<>(
                adminUserService.listUsers(type, query, PaginationUtil.getPageableRequest(page, size))));
    }

    @Operation(summary = "Cria um usuário de qualquer perfil",
            description = "Inclui outros ADMIN. SUPPLIER exige cnpj e cityId e nasce aguardando aprovação; "
                    + "DELIVERER exige supplierId e já entra na frota desse fornecedor. Só ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, e-mail já cadastrado ou campo do perfil faltando"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cidade ou fornecedor não encontrado")
    })
    @PostMapping
    public ResponseEntity<AdminUserDto> create(@Valid @RequestBody AdminCreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(dto));
    }

    @Operation(summary = "Desativa ou reativa a conta de um usuário",
            description = "Conta desativada não entra por senha nem por código e o token que já tinha deixa de valer. "
                    + "O admin não altera a própria conta. Só ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta atualizada"),
            @ApiResponse(responseCode = "400", description = "Corpo inválido ou tentativa de alterar a própria conta"),
            @ApiResponse(responseCode = "401", description = "Sem token válido"),
            @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<Void> setEnabled(
            @Parameter(description = "Id do usuário") @PathVariable String id,
            @Valid @RequestBody AdminSetEnabledDto dto) {
        adminUserService.setEnabled(id, dto.getEnabled(), loggedUser());
        return ResponseEntity.noContent().build();
    }

    private User loggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
