package com.h2gas.model.repository;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DelivererRepositoryTest {

    @Autowired
    private DelivererRepository delivererRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CityRepository cityRepository;

    @Test
    @DisplayName("Deve salvar entregador vinculado a usuário e supplier")
    void deveSalvarEntregadorVinculadoAUsuarioESupplier() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Entregador");
        supplier.setEmail("fornecedor.entregador@h2gas.test");
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("12345678000199");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(40));
        supplier.setActive(true);
        supplier.setCity(city);
        supplier = supplierRepository.saveAndFlush(supplier);

        User delivererUser = userRepository.saveAndFlush(new User(
                "Entregador Teste",
                "entregador@h2gas.test",
                "41977776666",
                "98765432100",
                "senha",
                UserTypeEnum.DELIVERER
        ));

        Deliverer deliverer = new Deliverer();
        deliverer.setUser(delivererUser);
        deliverer.setSupplier(supplier);
        deliverer.setVehicleInfo("Moto ABC-1234");

        Deliverer saved = delivererRepository.saveAndFlush(deliverer);

        assertNotNull(saved.getId());
        assertEquals(delivererUser.getId(), saved.getUser().getId());
        assertEquals(supplier.getId(), saved.getSupplier().getId());
        assertTrue(saved.isActive());
    }
}
