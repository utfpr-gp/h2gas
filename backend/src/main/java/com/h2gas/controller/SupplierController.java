package com.h2gas.controller;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import jakarta.validation.Valid;
import com.h2gas.model.dto.*;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.SupplierDashboardService;
import com.h2gas.services.SupplierService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;


@Tag(name = "Fornecedores")
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierMapper supplierMapper;

    private final SupplierDashboardService supplierDashboardService;

    public SupplierController(SupplierDashboardService supplierDashboardService) {
        this.supplierDashboardService = supplierDashboardService;
    }

    @Operation(summary = "Dashboard do fornecedor logado",
            description = "Indicadores do mês (receita, pedidos, ticket médio, variação sobre o mês anterior, receita a receber, "
                    + "pendentes, em rota, concluídas, tempo médio de entrega, vendas por dia, por produto e por forma de pagamento). "
                    + "O fornecedor vem do token; não há parâmetro para consultar o de outro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores do mês"),
            @ApiResponse(responseCode = "400", description = "Mês em formato inválido (use YYYY-MM) ou no futuro"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine/dashboard")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<SupplierDashboardDTO> getDashboard(
            @Parameter(description = "Mês no formato YYYY-MM; omitido = mês corrente", example = "2026-10")
            @RequestParam(value = "month", required = false) String month) {
        YearMonth yearMonth;
        try {
            yearMonth = month == null ? YearMonth.now() : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Mês inválido. Use o formato YYYY-MM.");
        }
        User supplier = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(supplierDashboardService.buildDashboard(supplier.getId(), yearMonth));
    }

    @Operation(summary = "Lista fornecedores com filtro de status e busca",
            description = "Lista paginada e ordenada por nome. Usada pelo admin na aprovação de fornecedores: "
                    + "active=false traz quem aguarda aprovação, active=true os aprovados e sem o parâmetro vêm todos. "
                    + "A busca procura no nome, e-mail ou CNPJ, sem diferenciar maiúsculas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de fornecedores"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel não permitido")
    })
    @GetMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<PageResponse<SupplierDTO>> getAllSuppliers(
            @Parameter(description = "false = aguardando aprovação, true = ativos; omitido = todos")
            @RequestParam(value = "active", required = false) Boolean active,
            @Parameter(description = "Trecho do nome, e-mail ou CNPJ", example = "gás centro")
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Número da página (0-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página (máx. 50)") @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.findAllSuppliers(active, q, pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<PageResponse<SupplierDTO>> searchSuppliers(
            @RequestParam String name,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.searchSuppliers(name, pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lista fornecedores ativos e abertos",
            description = "Retorna fornecedores ativos e abertos, opcionalmente filtrados por cidade do endereço ativo do cliente (RN05)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de fornecedores ativos e abertos")
    })
    @GetMapping("/actives/open")
    @ResponseBody
    public ResponseEntity<PageResponse<SupplierDTO>> getAllSuppliersOpenAndActive(
            @Parameter(description = "Número da página (0-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filtra por cidade do endereço ativo do cliente (RN05)") @RequestParam(value = "cityId", required = false) Long cityId
    ) {
        Pageable pageable = PaginationUtil.getPageableRequest(page, size);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(cityId, pageable);
        PageResponse<SupplierDTO> response = new PageResponse<>(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<List<SupplierSummaryDTO>> getAllSuppliersSummary() {
        List<SupplierSummaryDTO> supplierDTO = supplierService.findAllSuppliersSummary();
        return ResponseEntity.ok(supplierDTO);
    }

    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody SupplierCreateDTO supplierDTO) {
        SupplierDTO supplierCreate = supplierService.createSupplier(supplierDTO);
        return ResponseEntity.ok(supplierCreate);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<String> deleteSupplier(@PathVariable("id") String id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(id);
    }

    @PatchMapping("/{id}/update-active")
    @ResponseBody
    @RolesAllowed("ADMIN")
    public ResponseEntity<Map<String, String>> updateSupplierStatus(@PathVariable String id,
                                                     @RequestBody Map<String, Boolean> status) {
        boolean novoStatus = status.get("active");
        supplierService.updateStatusSupplier(id, novoStatus);
        return ResponseEntity.ok(Map.of("message", "Status atualizado."));
    }

    @Operation(summary = "Configurações da loja do fornecedor logado",
            description = "Horário de funcionamento, tempo médio de entrega e estoque mínimo. O fornecedor vem do token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações atuais"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine/settings")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<SupplierSettingsDTO> getMySettings() {
        return ResponseEntity.ok(supplierService.findMySettings(currentUser().getId()));
    }

    @Operation(summary = "Salva as configurações da loja do fornecedor logado",
            description = "Grava horário, tempo médio de entrega e estoque mínimo de uma vez. Fechamento antes da abertura "
                    + "significa que a loja fecha depois da meia-noite; igual à abertura é recusado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações salvas"),
            @ApiResponse(responseCode = "400", description = "Campo ausente, fora do limite ou fechamento igual à abertura"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @PutMapping("/mine/settings")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<SupplierSettingsDTO> updateMySettings(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Configurações da loja")
            @Valid @RequestBody SupplierSettingsDTO settings) {
        return ResponseEntity.ok(supplierService.updateMySettings(currentUser().getId(), settings));
    }

    @Operation(summary = "Atualiza horário e tempo de entrega de um fornecedor",
            description = "Campos opcionais (openingTime, closingTime, averageDeliveryTime em minutos). "
                    + "O fornecedor só altera a própria loja; o admin altera qualquer uma")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horário atualizado"),
            @ApiResponse(responseCode = "400", description = "Tempo médio de entrega não numérico"),
            @ApiResponse(responseCode = "403", description = "Fornecedor alterando outra loja"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado")
    })
    @PatchMapping("/{id}/update-times")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Map<String, String>> updateSupplierTimes(@Parameter(description = "Id do fornecedor") @PathVariable String id,
                                                                    @RequestBody Map<String, String> times) {
        User actor = currentUser();
        if (actor.getUserType() == UserTypeEnum.SUPPLIER && !actor.getId().equals(id)) {
            throw new ForbiddenResourceException("Você só pode alterar o horário da sua própria loja.");
        }
        supplierService.updateTimesSupplier(id, times);
        return ResponseEntity.ok(Map.of("message", "Horário de funcionamento atualizado."));
    }

    private static User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<SupplierDTO> getAddressById(@PathVariable String id) {
        SupplierDTO supplierDTO = supplierService.findById(id);
        return ResponseEntity.ok(supplierDTO);
    }

    @Operation(summary = "Lê os dados da loja do fornecedor logado",
            description = "Nome, telefone, descrição, CNPJ (só leitura), logo e endereço resumido. A loja vem do token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da loja"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @GetMapping("/mine/profile")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<SupplierProfileDTO> getMyProfile() {
        return ResponseEntity.ok(supplierService.findMyProfile(currentUser().getId()));
    }

    @Operation(summary = "Salva os dados da loja do fornecedor logado",
            description = "Só nome, telefone e descrição. Horário e entrega são da configuração; a cidade é do endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados salvos"),
            @ApiResponse(responseCode = "400", description = "Nome vazio ou longo, telefone inválido ou descrição longa"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @PutMapping("/mine/profile")
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<SupplierProfileDTO> updateMyProfile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados da loja")
            @Valid @RequestBody SupplierProfileRequestDTO request) {
        return ResponseEntity.ok(supplierService.updateMyProfile(currentUser().getId(), request));
    }

    @Operation(summary = "Envia a logo da loja do fornecedor logado",
            description = "Multipart com o campo file: JPG, PNG ou WEBP de até 1 MB. A logo aparece no card da loja no catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo salva; devolve logoUrl"),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio, de outro tipo ou maior que 1 MB"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER"),
            @ApiResponse(responseCode = "502", description = "Falha ao enviar a imagem ao Cloudinary")
    })
    @PutMapping(value = "/mine/logo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Map<String, String>> updateMyLogo(
            @Parameter(description = "Imagem da logo") @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(Map.of("logoUrl", supplierService.updateMyLogo(currentUser().getId(), file)));
    }

    @Operation(summary = "Remove a logo da loja do fornecedor logado", description = "O catálogo volta a mostrar o ícone de loja")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logo removida"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de SUPPLIER")
    })
    @DeleteMapping("/mine/logo")
    @RolesAllowed("SUPPLIER")
    public ResponseEntity<Void> removeMyLogo() {
        supplierService.removeMyLogo(currentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Edita um fornecedor",
            description = "O fornecedor só edita a própria loja; o admin edita qualquer uma. A tela da loja usa /mine/profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor atualizado"),
            @ApiResponse(responseCode = "403", description = "Fornecedor editando outra loja"),
            @ApiResponse(responseCode = "404", description = "Fornecedor ou cidade inexistente")
    })
    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<SupplierDTO> update(@Parameter(description = "Id do fornecedor") @PathVariable String id, @RequestBody SupplierCreateDTO dto) {
        User actor = currentUser();
        if (actor.getUserType() == UserTypeEnum.SUPPLIER && !actor.getId().equals(id)) {
            throw new ForbiddenResourceException("Você só pode editar a sua própria loja.");
        }
        SupplierDTO updated = supplierService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Abre ou fecha a loja", description = "Corpo {\"status\": true|false}. O fornecedor só altera a própria loja")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "403", description = "Fornecedor alterando outra loja")
    })
    @PatchMapping("/{id}/status")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Map<String, String>> changeStatusOpenClosed(@PathVariable String id,
                                                              @RequestBody Map<String, Boolean> status) {
        User actor = currentUser();
        if (actor.getUserType() == UserTypeEnum.SUPPLIER && !actor.getId().equals(id)) {
            throw new ForbiddenResourceException("Você só pode abrir ou fechar a sua própria loja.");
        }
        boolean novoStatus = status.get("status");
        supplierService.updateStatusOpenClosed(id, novoStatus);
        return ResponseEntity.ok(Map.of("message", "Status atualizado."));
    }

    @GetMapping("/{id}/active")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Boolean> getSupplierStatus(@PathVariable String id) {
        boolean active = supplierService.findById(id).isActive();
        return ResponseEntity.ok(active);
    }

    @GetMapping("/{id}/open")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<Boolean> getSupplierOpen(@PathVariable String id) {
        boolean active = supplierService.findById(id).isOpen();
        return ResponseEntity.ok(active);
    }
}
