package com.h2gas.services;

import com.h2gas.model.Coordinates;
import com.h2gas.model.entity.Address;
import com.h2gas.model.repository.AddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// HU17: na subida do backend, os endereços antigos ganham coordenada (decisão do autor, 2026-10-28)
class AddressCoordinatesBackfillTest {

    private Address endereco(long id, String cep) {
        Address a = new Address();
        a.setId(id);
        a.setZipCode(cep);
        return a;
    }

    @Test
    @DisplayName("HU17: preenche só os endereços sem coordenada")
    void devePreencherSoOsEnderecosSemCoordenada() {
        AddressRepository repo = mock(AddressRepository.class);
        GeocodingService geo = mock(GeocodingService.class);
        Address semCoordenada = endereco(1, "80020310");
        when(repo.findByLatitudeIsNull()).thenReturn(List.of(semCoordenada));
        when(geo.findByZipCode("80020310")).thenReturn(Optional.of(new Coordinates(-25.4296, -49.2713)));

        int preenchidos = new AddressCoordinatesBackfill(repo, geo, 0).fillMissing();

        assertEquals(1, preenchidos);
        assertEquals(0, new BigDecimal("-25.4296000").compareTo(semCoordenada.getLatitude()));
        verify(repo).save(semCoordenada);
    }

    @Test
    @DisplayName("HU17: um endereço que falha não interrompe os outros")
    void deveSeguirQuandoUmEnderecoFalha() {
        AddressRepository repo = mock(AddressRepository.class);
        GeocodingService geo = mock(GeocodingService.class);
        Address ruim = endereco(1, "00000000");
        Address bom = endereco(2, "80420090");
        when(repo.findByLatitudeIsNull()).thenReturn(List.of(ruim, bom));
        when(geo.findByZipCode("00000000")).thenThrow(new IllegalStateException("falha inesperada"));
        when(geo.findByZipCode("80420090")).thenReturn(Optional.of(new Coordinates(-25.4459, -49.2867)));

        int preenchidos = new AddressCoordinatesBackfill(repo, geo, 0).fillMissing();

        assertEquals(1, preenchidos);
        verify(repo, times(1)).save(any());
        assertNull(ruim.getLatitude());
    }
}
