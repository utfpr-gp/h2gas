package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.entity.Address;
import com.h2gas.model.repository.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.mockito.Mockito.when;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class CityServiceTest {

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @MockitoBean
    private IbgeClient ibgeClient;

    private Long validCityId;

    @BeforeEach
    public void setUp() {
        City cityFirst = new City(
                null,
                "Curitiba",
                "PR"
        );
        cityRepository.save(cityFirst);

        City citySecond = new City(
                null,
                "Guarapuava",
                "PR"
        );

        cityRepository.save(citySecond);

        validCityId = cityFirst.getId();
    }

    @Test
    @DisplayName("Procura todas as cidades")
    void findAllCities() {
        List<CityDTO> result = cityService.findAllCities();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("Curitiba", result.get(0).getName());
        assertEquals("Guarapuava", result.get(1).getName());
    }

    @Test
    @DisplayName("Procura cidades utilizando o id")
    void findByIdCitySucess() {
        CityDTO result = cityService.findByIdCity(validCityId);
        assertNotNull(result);
        assertEquals("Curitiba", result.getName());
    }

    @Test
    @DisplayName("Retorna erro ao passar o id incorreto")
    void findByIdCityError() {
        Long idInvalid = 999L;
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cityService.findByIdCity(idInvalid);
        });
        assertEquals("Cidade não encontra com esse id: " + idInvalid, exception.getMessage());
    }

    @Test
    @DisplayName("Deve deletar cidade")
    void deleteCity() {
        assertTrue(cityRepository.findById(validCityId).isPresent());
        cityService.deleteCity(validCityId);
        assertFalse(cityRepository.findById(validCityId).isPresent());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar cidade inexistente")
    void deleteCityNotFound() {
        Long invalidId = 99L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            cityService.deleteCity(invalidId);
        });

        assertEquals("Cidade com id " + invalidId + " não encontrada", exception.getMessage());
    }


    @Test
    @DisplayName("Deve encontra a cidade pelo nome")
    void findByNameSuccess() {
        CityDTO result = cityService.findByName("Curitiba");
        assertNotNull(result);
        assertEquals("Curitiba", result.getName());
    }

    @Test
    @DisplayName("Deve criar uma nova cidade com sucesso")
    void createCitySuccess() {
        when(ibgeClient.findMunicipality(4113700)).thenReturn(Optional.of(new IbgeClient.Municipality(4113700, "Londrina", "PR")));

        CityDTO cidadeCriada = cityService.createCity(4113700);

        assertNotNull(cidadeCriada);
        assertNotNull(cidadeCriada.getId());
        assertEquals("Londrina", cidadeCriada.getName());
        assertEquals("PR", cidadeCriada.getState());

        City entidade = cityRepository.findById(cidadeCriada.getId()).orElse(null);
        assertNotNull(entidade);
        assertEquals("Londrina", entidade.getName());
    }

    @Test
    @DisplayName("F7 fatia 6: nome e UF vêm do IBGE, com o código guardado")
    void deveCadastrarCidadeComNomeEUfDoIbge() {
        when(ibgeClient.findMunicipality(4125506)).thenReturn(Optional.of(new IbgeClient.Municipality(4125506, "São José dos Pinhais", "PR")));

        CityDTO criada = cityService.createCity(4125506);

        City entidade = cityRepository.findById(criada.getId()).orElseThrow();
        assertEquals("São José dos Pinhais", entidade.getName());
        assertEquals("PR", entidade.getState());
        assertEquals(4125506, entidade.getIbgeCode());
        assertEquals(4125506, criada.getIbgeCode());
    }

    @Test
    @DisplayName("F7 fatia 6: cidade já atendida é 409")
    void deveRecusarCidadeJaAtendida() {
        City existente = cityRepository.findById(validCityId).orElseThrow();
        existente.setIbgeCode(4106902);
        cityRepository.save(existente);
        when(ibgeClient.findMunicipality(4106902)).thenReturn(Optional.of(new IbgeClient.Municipality(4106902, "Curitiba", "PR")));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> cityService.createCity(4106902));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals("Curitiba - PR já é atendida.", erro.getReason());
    }

    @Test
    @DisplayName("F7 fatia 6: cidade antiga sem código, mesmo nome e UF, também é 409")
    void deveRecusarCidadeAntigaComMesmoNomeEUf() {
        when(ibgeClient.findMunicipality(4109401)).thenReturn(Optional.of(new IbgeClient.Municipality(4109401, "Guarapuava", "PR")));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> cityService.createCity(4109401));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    @DisplayName("F7 fatia 6: código que não existe no IBGE é 400")
    void deveRecusarCodigoInexistenteNoIbge() {
        when(ibgeClient.findMunicipality(9999999)).thenReturn(Optional.empty());

        BusinessRuleException erro = assertThrows(BusinessRuleException.class, () -> cityService.createCity(9999999));

        assertEquals("Município não encontrado no IBGE.", erro.getMessage());
    }

    @Test
    @DisplayName("F7 fatia 6: cidade com fornecedor ou endereço não pode ser excluída")
    void deveRecusarExcluirCidadeComFornecedorOuEndereco() {
        City curitiba = cityRepository.findById(validCityId).orElseThrow();
        addressRepository.save(new Address(null, "Rua XV", "casa", "10", "perto", "Centro", "80010000", curitiba, null));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> cityService.deleteCity(validCityId));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals("Curitiba - PR tem fornecedores ou endereços cadastrados e não pode ser excluída.", erro.getReason());
        assertTrue(cityRepository.findById(validCityId).isPresent());
    }
}