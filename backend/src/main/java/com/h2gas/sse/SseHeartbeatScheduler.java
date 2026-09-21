package com.h2gas.sse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SseHeartbeatScheduler {

    private final SseEmitterService sseEmitterService;

    public SseHeartbeatScheduler(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        sseEmitterService.publishHeartbeatToAllChannels();
    }
}
