package com.pos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// DISABLING DATABASE FOR TEST PUSH #30
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@RestController
public class PosServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosServerApplication.class, args);
	}

	@GetMapping("/")
	public String health() {
		return "🚀 POS BACKEND IS FINALLY LIVE!";
	}
}
