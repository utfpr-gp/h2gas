package com.h2gas.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterService {

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByChannel = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = newEmitter();
        emittersByChannel.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> emitters = emittersByChannel.get(channel);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    emittersByChannel.remove(channel, emitters);
                }
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        return emitter;
    }

    public void publish(String channel, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByChannel.getOrDefault(channel, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    public void publishHeartbeatToAllChannels() {
        for (List<SseEmitter> emitters : emittersByChannel.values()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        }
    }

    SseEmitter newEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }
}
