package com.h2gas.services;

import com.h2gas.model.entity.Address;
import com.h2gas.model.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// HU17: na subida do backend, os endereços que ainda não têm coordenada ganham a do CEP (decisão do autor, 2026-10-28).
// Roda numa thread à parte, um endereço por vez com pausa curta, e não segura o boot.
@Component
@Profile("!test")
public class AddressCoordinatesBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AddressCoordinatesBackfill.class);

    private final AddressRepository addressRepository;
    private final GeocodingService geocodingService;
    private final long pauseMs;

    @Autowired
    public AddressCoordinatesBackfill(AddressRepository addressRepository, GeocodingService geocodingService,
                                      @Value("${geocoding.backfill-pause-ms:300}") long pauseMs) {
        this.addressRepository = addressRepository;
        this.geocodingService = geocodingService;
        this.pauseMs = pauseMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread worker = new Thread(() -> log.info("Coordenadas preenchidas em {} endereço(s)", fillMissing()), "address-coordinates");
        worker.setDaemon(true);
        worker.start();
    }

    public int fillMissing() {
        int filled = 0;
        for (Address address : addressRepository.findByLatitudeIsNull()) {
            try {
                var coordinates = geocodingService.findByZipCode(address.getZipCode());
                if (coordinates.isPresent()) {
                    address.applyCoordinates(coordinates.get());
                    addressRepository.save(address);
                    filled++;
                }
                if (pauseMs > 0) Thread.sleep(pauseMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return filled;
            } catch (RuntimeException e) {
                log.warn("Não foi possível preencher as coordenadas do endereço {}: {}", address.getId(), e.getMessage());
            }
        }
        return filled;
    }
}
