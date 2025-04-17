package com.h2gas.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
