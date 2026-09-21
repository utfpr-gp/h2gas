package com.h2gas.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @Test
    @DisplayName("Deve registrar emitter ao inscrever")
    void deveRegistrarEmitterAoInscrever() throws IOException {
        SseEmitterService service = spy(new SseEmitterService());
        SseEmitter emitter = mock(SseEmitter.class);
        doReturn(emitter).when(service).newEmitter();

        SseEmitter inscrito = service.subscribe("order:123");

        org.junit.jupiter.api.Assertions.assertSame(emitter, inscrito);
        service.publish("order:123", "status", Map.of("id", "123"));
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("Deve publicar evento para todos os emitters do canal")
    void devePublicarEventoParaTodosOsEmittersDoCanal() throws IOException {
        SseEmitterService service = spy(new SseEmitterService());
        SseEmitter primeiro = mock(SseEmitter.class);
        SseEmitter segundo = mock(SseEmitter.class);
        doReturn(primeiro, segundo).when(service).newEmitter();

        service.subscribe("order:123");
        service.subscribe("order:123");
        Map<String, String> payload = Map.of("status", "OUT_FOR_DELIVERY");
        service.publish("order:123", "order-updated", payload);

        verify(primeiro).send(any(SseEmitter.SseEventBuilder.class));
        verify(segundo).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("Deve remover emitter ao completar")
    void deveRemoverEmitterAoCompletar() throws IOException {
        SseEmitterService service = spy(new SseEmitterService());
        SseEmitter emitter = mock(SseEmitter.class);
        doReturn(emitter).when(service).newEmitter();

        service.subscribe("order:123");

        ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(completion.capture());
        assertDoesNotThrow(() -> completion.getValue().run());

        service.publish("order:123", "status", Map.of("gone", "true"));
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
