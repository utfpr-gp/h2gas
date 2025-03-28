package com.h2gas.util;

import io.github.cdimascio.dotenv.Dotenv;

public class ConfigEnv {

    public static void setEnv(Dotenv dotenv) {
		System.setProperty("SPRING_DATASOURCE_USERNAME", dotenv.get("SPRING_DATASOURCE_USERNAME"));
		System.setProperty("SPRING_DATASOURCE_PASSWORD", dotenv.get("SPRING_DATASOURCE_PASSWORD"));
		System.setProperty("MYSQL_HOST", dotenv.get("MYSQL_HOST"));
		System.setProperty("MYSQL_PORT", dotenv.get("MYSQL_PORT"));
		System.setProperty("MYSQL_DATABASE", dotenv.get("MYSQL_DATABASE"));
    }

}
