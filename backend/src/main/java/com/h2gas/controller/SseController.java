package com.h2gas.controller;

import com.h2gas.model.entity.User;
import com.h2gas.sse.SseChannels;
import com.h2gas.sse.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE")
@RestController
@RequestMapping("/api/sse")
@SecurityRequirement(name = "bearerAuth")
public class SseController {

    private final SseEmitterService sseEmitterService;

    public SseController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping(value = "/subscribe/{channel}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Inscreve em um canal SSE",
            description = "Mantém uma conexão text/event-stream aberta por até 30 minutos de inatividade. "
                    + "O EventSource do navegador não envia header Authorization; nesses casos o JWT deve ir em ?token=. "
                    + "Canal do acompanhamento de pedidos (HU08): customer:{customerId}, só o do próprio usuário do token; "
                    + "o evento order-status-changed traz { orderId, status }."
    )
    @Parameter(
            name = "token",
            in = ParameterIn.QUERY,
            description = "JWT equivalente ao Bearer, exclusivo para EventSource nativo nas rotas /api/sse/**",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream SSE aberto",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Canal de outro cliente ou fora da convenção customer:{customerId}")
    })
    public SseEmitter subscribe(
            @Parameter(description = "Canal do cliente logado: customer:{customerId}", example = "customer:d124410e-f6d4-4a24-8631-a69ba4aad772")
            @PathVariable String channel,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        // HU08: sem isso qualquer usuário logado ouviria os pedidos de outro cliente
        if (user == null || !SseChannels.isOwnCustomerChannel(channel, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Canal não pertence ao usuário logado.");
        }
        return sseEmitterService.subscribe(channel);
    }
}
