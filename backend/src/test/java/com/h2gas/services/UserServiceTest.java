package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//@DataJpaTest
@ExtendWith(MockitoExtension.class)
//@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @MockitoBean
    private TokenService tokenService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserMapper userMapper;
//    @Spy
//    private UserMapper userMapper = new UserMapper();

    @Test
    @Transactional
    @DisplayName("Procura users pelo id do Banco de dados")
    void findByIdUSerCase1() {
        String idTest = "94c4c7ca-6dca-4bc5-80ee-5f564918ca07";
        User dataMock = new User(
                idTest,
                "Pablo Teste",
                "pablo@gmail.com",
                "1234545",
                "123435345",
                "234234234",
                "4534534",
                UserTypeEnum.CUSTOMER);

        UserDto userDtoMock = new UserDto();
        userDtoMock.setEmail("pablo@gmail.com");

        when(userRepository.findById(idTest)).thenReturn(Optional.of(dataMock));
        when(userMapper.toDto(dataMock)).thenReturn(userDtoMock);

        UserDto result = userService.findByIdUSer(idTest);

        assertEquals("pablo@gmail.com", result.getEmail());
//        verify(userRepository).findById(idTest);
//        verify(userMapper).toDto(dataMock);
    }

    @Test
    public void findByIdUSerCase2Exception() {
        String idTest = "94c4c7ca-6dca-4bc5-80ee-5f564918ca07";
        String idErrado = "94c4c7ca-6dca-4bc5-80ee-5f564918ca05";

        when(userRepository.findById(idErrado)).thenReturn(Optional.empty());

        Assertions.assertThrows(RuntimeException.class, () -> {
            this.userService.findByIdUSer(idErrado);
        });

        verify(userRepository).findById(idErrado);
    }
}