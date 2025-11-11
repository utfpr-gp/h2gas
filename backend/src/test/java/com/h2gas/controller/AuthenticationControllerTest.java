package com.h2gas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.AuthenticationDTO;
import com.h2gas.model.dto.RegisterDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;


@SpringBootTest
@Import(SecurityConfigurations.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class AuthenticationControllerTest {
    @MockitoBean
    private MailService mailService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean //trocar para Autowired quando for testar utilizando o h2db
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private AddressRepository addressRepository;


//    @MockitoBean
//    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private CityRepository cityRepository;

    private Long validCityId;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setEmail("teste@exemplo.com");
        user.setId("94c4c7ca-6dca-4bc5-80ee-5f564918ca07");
        user.setUserType(UserTypeEnum.CUSTOMER);
        user.setPassword(passwordEncoder.encode("123456"));
        userRepository.save(user);

        City cityFirst = new City(
                null,
                "Pato Branco",
                "PR"
        );
        cityRepository.save(cityFirst);
        validCityId = cityFirst.getId();

        var authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authToken);
    }

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void retornaTokenAoEfetuarLogin() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO("teste@exemplo.com", "123456");
        when(tokenService.generateToken(any(User.class))).thenReturn("fake-token-123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void deveRegistrarUsuarioComSucesso() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("d4154c80-c616-4471-a0d8-04b29d30345f");
            return user;
        });

        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(1L);
            return address;
        });


        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreetName("Rua Teste");
        addressDTO.setNumber("123");
        addressDTO.setNeighborhood("Centro");
        addressDTO.setZipCode("80000-000");
        addressDTO.setCityId(validCityId);

        RegisterDTO registerDTO = new RegisterDTO(
                "teste",
                "teste@teste.com",
                "12323423423",
                "12345678901",
                "12345678000199",
                "123456",
                UserTypeEnum.CUSTOMER,
                addressDTO
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void naoDeveRegistrarUsuarioSeEmailJaExiste() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO(
                "João da Silva",
                "teste@email.com",
                "41999999999",
                "12345678901",
                "12345678000199",
                "123456",
                UserTypeEnum.CUSTOMER,
                null
        );

//        when(userRepository.findByEmail("teste@email.com"))
//                .thenReturn(Optional.of(new User()));
        when(userRepository.existsByEmail("teste@email.com"))
                .thenReturn(true);
        System.out.println("Mock existsByEmail: " + userRepository.existsByEmail("teste@email.com"));


        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }
}
