package com.h2gas.services.impl;

import com.h2gas.dto.UserDto;
import com.h2gas.entity.UserEntity;
import jakarta.persistence.EntityManager;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    EntityManager entityManager;

    @Test
    public void findAllUsers() {

    }

    @Test
    void findByIdUSer() {
    }

//    private UserEntity createUser(UserDto userDto) {
//        UserEntity userEntity = new UserEntity(userDto);
//    }
}