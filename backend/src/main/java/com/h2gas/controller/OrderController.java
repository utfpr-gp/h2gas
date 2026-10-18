package com.h2gas.controller;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.dto.AssignDelivererRequestDTO;
import com.h2gas.model.dto.CancelDeliveryRequestDTO;
import com.h2gas.model.dto.CancelOrderRequestDTO;
import com.h2gas.model.dto.CancelOrderResponseDTO;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.services.DelivererService;
import com.h2gas.services.OrderNotificationService;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.dto.ReorderPreviewDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.response.PageResponse;
import com.h2gas.services.OrderService;
import com.h2gas.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Pedidos")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderNotificationService orderNotificationService;

    @Autowired
    private DelivererService delivererService;

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

    @Operation(summary = "Cria um pedido",
            description = "Cria o pedido com status PENDING (RN03). O preço de cada item é calculado no backend (RN01) "
                    + "conforme a modalidade: DELIVERY usa o preço de entrega, PICKUP o de retirada; ausente = DELIVERY. "
                    + "DELIVERY exige addressId do próprio cliente; PICKUP não tem endereço de entrega. "
                    + "Decrementa o estoque na mesma transação (RN02)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente, endereço inválido ou modalidade desconhecida"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel sem permissão"),
            @ApiResponse(responseCode = "404", description = "Cliente, fornecedor, endereço ou produto inexistente")
    })
    @PostMapping
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO orderResponseDTO = orderService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(orderResponseDTO);
    }

    @Operation(summary = "Pré-visualiza o subtotal do carrinho",
            description = "Calcula no backend (RN01) o preço unitário e o total de cada item conforme a modalidade "
                    + "(DELIVERY ou PICKUP) e o subtotal. Não cria pedido nem mexe no estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtotal calculado"),
            @ApiResponse(responseCode = "400", description = "Sem itens, quantidade inválida ou modalidade desconhecida"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel sem permissão"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente")
    })
    @PostMapping("/preview")
    @ResponseBody
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public ResponseEntity<OrderPreviewDTO> previewOrder(@Valid @RequestBody OrderPreviewRequestDTO request) {
        return ResponseEntity.ok(orderService.previewOrder(request));
    }

    @Operation(summary = "Monta a recompra de um pedido (Pedir Novamente)",
            description = "Para o cliente dono do pedido, devolve os itens que podem voltar ao carrinho com dados e "
                    + "preços atuais do produto (quantidade limitada ao estoque) e os que ficaram de fora "
                    + "(removidos ou sem estoque). Não altera estoque nem cria pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itens disponíveis e indisponíveis para a recompra"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro cliente"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente")
    })
    @GetMapping("/{id}/reorder-preview")
    @ResponseBody
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public ResponseEntity<ReorderPreviewDTO> getReorderPreview(
            @Parameter(description = "ID do pedido a refazer") @PathVariable String id) {
        User usuarioLogado = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(orderService.buildReorderPreview(id, usuarioLogado));
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

    @Operation(summary = "Cancela um pedido com justificativa (fornecedor)",
            description = "Só o fornecedor dono do pedido (ou um ADMIN) cancela. Exige justificativa, devolve o estoque "
                    + "dos itens na mesma transação e tenta avisar o cliente por e-mail; se o e-mail falhar, o pedido "
                    + "continua cancelado e a resposta vem com customerNotified=false")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "400", description = "Sem justificativa, ou pedido já entregue/cancelado"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente")
    })
    @PatchMapping("/{id}/cancel")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<CancelOrderResponseDTO> cancelOrder(
            @Parameter(description = "ID do pedido") @PathVariable String id,
            @Valid @RequestBody CancelOrderRequestDTO request) {
        User usuarioLogado = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Order cancelado = orderService.cancelOrder(id, request.getReason(), usuarioLogado);
        boolean customerNotified = orderNotificationService.notifyCancellation(cancelado);
        return ResponseEntity.ok(new CancelOrderResponseDTO(cancelado.getStatus().getStatus(), customerNotified));
    }

    @Operation(summary = "Despacha o pedido atribuindo um entregador (fornecedor)",
            description = "Só pedido de entrega em preparação (IN_PREPARATION) do próprio fornecedor (ou ADMIN). "
                    + "O entregador precisa ser ativo e da frota do fornecedor. Apenas atribui: o pedido segue em preparação até o "
                    + "entregador iniciar a entrega (start-delivery). Chamar de novo troca o entregador. "
                    + "Entregador já em rota pode receber outro pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido despachado"),
            @ApiResponse(responseCode = "400", description = "Pedido fora de preparação ou de retirada; entregador de outro fornecedor ou inativo"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro fornecedor"),
            @ApiResponse(responseCode = "404", description = "Pedido ou entregador inexistente")
    })
    @PatchMapping("/{id}/assign-deliverer")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER"})
    public ResponseEntity<OrderResponseDTO> assignDeliverer(
            @Parameter(description = "ID do pedido") @PathVariable String id,
            @Valid @RequestBody AssignDelivererRequestDTO request) {
        return ResponseEntity.ok(orderService.assignDeliverer(id, request.getDelivererId(), loggedUser()));
    }

    @Operation(summary = "Rotas ativas do entregador logado",
            description = "Pedidos atribuídos ao entregador autenticado: em rota (OUT_FOR_DELIVERY, do despacho mais antigo) e depois os "
                    + "pendentes de início (IN_PREPARATION, do pedido mais antigo); o status distingue EM ROTA de PENDENTE. "
                    + "O entregador vem do token; não há parâmetro para consultar rotas de outro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rotas do entregador"),
            @ApiResponse(responseCode = "403", description = "Sem token válido ou papel diferente de DELIVERER"),
            @ApiResponse(responseCode = "404", description = "Usuário logado não é um entregador cadastrado")
    })
    @GetMapping("/my-routes")
    @ResponseBody
    @RolesAllowed("DELIVERER")
    public ResponseEntity<List<OrderResponseDTO>> myRoutes() {
        Deliverer entregador = delivererService.findByUserId(loggedUser().getId());
        return ResponseEntity.ok(orderService.findActiveByDeliverer(entregador.getId()));
    }

    @Operation(summary = "Inicia a entrega (entregador)",
            description = "O entregador logado inicia um pedido atribuído a ele e em preparação: vai a OUT_FOR_DELIVERY e registra dispatchedAt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega iniciada"),
            @ApiResponse(responseCode = "400", description = "Pedido não está em preparação"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro entregador"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente ou usuário não é entregador")
    })
    @PatchMapping("/{id}/start-delivery")
    @ResponseBody
    @RolesAllowed("DELIVERER")
    public ResponseEntity<OrderResponseDTO> startDelivery(@Parameter(description = "ID do pedido") @PathVariable String id) {
        Deliverer entregador = delivererService.findByUserId(loggedUser().getId());
        return ResponseEntity.ok(orderService.startDelivery(id, entregador.getId()));
    }

    @Operation(summary = "Cancela a entrega (entregador)",
            description = "O entregador logado recusa um pedido atribuído a ele, com motivo obrigatório. O pedido volta a "
                    + "IN_PREPARATION sem entregador para o fornecedor despachar de novo; o pedido não é cancelado ao cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega devolvida ao fornecedor"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente ou maior que 500 caracteres; pedido já entregue ou cancelado"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro entregador"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente ou usuário não é entregador")
    })
    @PatchMapping("/{id}/cancel-delivery")
    @ResponseBody
    @RolesAllowed("DELIVERER")
    public ResponseEntity<OrderResponseDTO> cancelDelivery(
            @Parameter(description = "ID do pedido") @PathVariable String id,
            @Valid @RequestBody CancelDeliveryRequestDTO request) {
        Deliverer entregador = delivererService.findByUserId(loggedUser().getId());
        return ResponseEntity.ok(orderService.cancelDelivery(id, entregador.getId(), request.getReason()));
    }

    @Operation(summary = "Confirma a entrega (entregador)",
            description = "O entregador logado confirma um pedido atribuído a ele e em rota: vai a DELIVERED e registra deliveredAt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega confirmada"),
            @ApiResponse(responseCode = "400", description = "Pedido não está em rota"),
            @ApiResponse(responseCode = "403", description = "Sem token válido, papel sem permissão ou pedido de outro entregador"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente ou usuário não é entregador")
    })
    @PatchMapping("/{id}/complete-delivery")
    @ResponseBody
    @RolesAllowed("DELIVERER")
    public ResponseEntity<OrderResponseDTO> completeDelivery(@Parameter(description = "ID do pedido") @PathVariable String id) {
        Deliverer entregador = delivererService.findByUserId(loggedUser().getId());
        return ResponseEntity.ok(orderService.completeDelivery(id, entregador.getId()));
    }

    private User loggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
