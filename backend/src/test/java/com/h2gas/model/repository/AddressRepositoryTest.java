package com.h2gas.model.repository;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Test
    @DisplayName("HU04: deve retornar todos os endereços de um usuário, do mais recente ao mais antigo")
    void deveRetornarTodosOsEnderecosDeUmUsuario() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        User user = userRepository.save(new User(
                "Cliente Repo", "cliente.repo@h2gas.test", "41988880000",
                "12345678905", "senha", UserTypeEnum.CUSTOMER));
        User outro = userRepository.save(new User(
                "Outro Repo", "outro.repo@h2gas.test", "41988880001",
                "12345678906", "senha", UserTypeEnum.CUSTOMER));

        Address casa = addressRepository.saveAndFlush(new Address(
                null, "Rua Um", "Casa", "10", null, "Centro", "80000-000", city, user));
        Address trabalho = addressRepository.saveAndFlush(new Address(
                null, "Rua Dois", "Trabalho", "20", null, "Batel", "80420-000", city, user));
        addressRepository.saveAndFlush(new Address(
                null, "Rua Três", "Casa", "30", null, "Centro", "80000-000", city, outro));

        List<Address> result = addressRepository.findAllByUserIdOrderByIdDesc(user.getId());

        assertEquals(2, result.size());
        assertEquals(trabalho.getId(), result.get(0).getId());
        assertEquals(casa.getId(), result.get(1).getId());
    }
}
