package com.h2gas;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();

//		System.setProperty("SPRING_DATASOURCE_USERNAME", dotenv.get("SPRING_DATASOURCE_USERNAME"));
//		System.setProperty("SPRING_DATASOURCE_PASSWORD", dotenv.get("SPRING_DATASOURCE_PASSWORD"));
//		System.setProperty("MYSQL_HOST", dotenv.get("MYSQL_HOST"));
//		System.setProperty("MYSQL_PORT", dotenv.get("MYSQL_PORT"));
//		System.setProperty("MYSQL_DATABASE", dotenv.get("MYSQL_DATABASE"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
