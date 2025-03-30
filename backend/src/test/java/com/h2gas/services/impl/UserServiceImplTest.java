package com.h2gas.services.impl;

import com.h2gas.dto.RegisterDTO;
import com.h2gas.dto.UserDto;
import com.h2gas.entity.UserEntity;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.mapper.UserMapper;
import com.h2gas.repository.UserRepository;
import com.h2gas.services.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    EntityManager entityManager;

//    @Autowired
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("Procura users pelo id do Banco de dados")
    void findByIdUSerCase1() {
        String idTest = "94c4c7ca-6dca-4bc5-80ee-5f564918ca07";
        UserEntity dataMock = new UserEntity(idTest,"Pablo Teste", "pablo@gmail.com",
                "1234545", "123435345", "234234234", "4534534", UserTypeEnum.CUSTOMER);

        when(userRepository.findById(idTest)).thenReturn(Optional.of(dataMock));
        UserDto userCreatTest = this.userService.findByIdUSer(idTest);

        assertEquals(dataMock.getEmail(), userCreatTest.email());
    }

    @Test
    public void findByIdUSerCase2Exception() {
        String idTest = "94c4c7ca-6dca-4bc5-80ee-5f564918ca07";
        String idErrado = "94c4c7ca-6dca-4bc5-80ee-5f564918ca05";
        UserEntity dataMock = new UserEntity(idTest,"Pablo Teste", "pablo@gmail.com",
                "1234545", "123435345", "234234234", "4534534", UserTypeEnum.CUSTOMER);

        when(userRepository.findById(idTest)).thenReturn(Optional.of(dataMock));
        UserDto userCreatTest = this.userService.findByIdUSer(idTest);

        Assertions.assertThrows(RuntimeException.class, () -> this.userService.findByIdUSer(idErrado));
    }
}