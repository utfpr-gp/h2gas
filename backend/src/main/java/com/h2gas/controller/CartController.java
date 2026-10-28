package com.h2gas.controller;

import com.h2gas.model.dto.CartPreviewDTO;
import com.h2gas.model.dto.CartPreviewRequestDTO;
import com.h2gas.services.CartPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Carrinho")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartPreviewService cartPreviewService;

    public CartController(CartPreviewService cartPreviewService) {
        this.cartPreviewService = cartPreviewService;
    }

    @Operation(summary = "Pré-visualiza o carrinho com dados e preços atuais",
            description = "Público (cliente e visitante). O navegador guarda só id e quantidade; esta rota devolve marca, "
                    + "foto, preço pela modalidade (RN01), estoque e se cada item pode ser comprado agora. Item "
                    + "indisponível (NOT_FOUND, STORE_INACTIVE, STORE_CLOSED, OUT_OF_STOCK) vem com o motivo e fica "
                    + "fora do subtotal.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = CartPreviewRequestDTO.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho com os dados atuais",
                    content = @Content(schema = @Schema(implementation = CartPreviewDTO.class))),
            @ApiResponse(responseCode = "400", description = "Lista vazia, mais de 50 itens, quantidade menor que 1 ou modalidade inválida",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/preview")
    public ResponseEntity<CartPreviewDTO> preview(@Valid @RequestBody CartPreviewRequestDTO request) {
        return ResponseEntity.ok(cartPreviewService.preview(request));
    }
}
