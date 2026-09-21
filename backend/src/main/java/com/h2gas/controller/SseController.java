package com.h2gas.controller;

import com.h2gas.sse.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
                    + "Convenção de canal para acompanhamento de pedido: order:{orderId}."
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
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public SseEmitter subscribe(
            @Parameter(description = "Identificador do canal. Convenção de pedido: order:{orderId}", example = "order:123")
            @PathVariable String channel
    ) {
        return sseEmitterService.subscribe(channel);
    }
}
