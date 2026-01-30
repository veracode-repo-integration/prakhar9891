package com.pos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PosServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosServerApplication.class, args);
	}

	@javax.annotation.PostConstruct
	public void debugStartup() {
		System.out.println("🚀 APPLICATION STARTING ON PORT: " + System.getenv("PORT"));
		System.out.println("🐘 DATABASE URL IN USE: " + System.getProperty("spring.datasource.url"));
	}
}
