package com.h2gas.services;

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
        Long idInvalid = 4L;
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
        Long invalidId = 999L;

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
        CityDTO novaCidade = new CityDTO(null, "Londrina", "PR");

        CityDTO cidadeCriada = cityService.createCity(novaCidade);

        assertNotNull(cidadeCriada);
        assertNotNull(cidadeCriada.getId());
        assertEquals("Londrina", cidadeCriada.getName());
        assertEquals("PR", cidadeCriada.getState());

        City entidade = cityRepository.findById(cidadeCriada.getId()).orElse(null);
        assertNotNull(entidade);
        assertEquals("Londrina", entidade.getName());
    }
}