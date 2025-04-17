package com.h2gas.services.impl;

import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

//@DataJpaTest
@SpringBootTest
public class UserServiceHDBTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    private String validUserId;

    @BeforeEach
    public void setUp() {
        User user = new User(null,"Pablo Teste", "pablo@gmail.com", "1234545", "123435345", "234234234", "4534534", UserTypeEnum.CUSTOMER, null);

        userRepository.save(user);

        validUserId = user.getId();
    }

    @Test
    public void testFindByIdUSerWithExistingUser() {
        UserDto result = userService.findByIdUSer(validUserId);

        assertNotNull(result);
        assertEquals("Pablo Teste", result.getName());
        assertEquals("pablo@gmail.com", result.getEmail());
    }

    @Test
    public void testFindByIdUSerWithNonExistingUser() {
        String invalidId = "94c4c7ca-6dca-4bc5-80ee-5f564918ca05";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findByIdUSer(invalidId);
        });

        assertEquals("User not found with id: " + invalidId, exception.getMessage());
    }
}