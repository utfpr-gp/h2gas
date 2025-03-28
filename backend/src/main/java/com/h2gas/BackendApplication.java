package com.h2gas;

import com.h2gas.util.ConfigEnv;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		ConfigEnv.setEnv(dotenv);
		SpringApplication.run(BackendApplication.class, args);
	}

}
