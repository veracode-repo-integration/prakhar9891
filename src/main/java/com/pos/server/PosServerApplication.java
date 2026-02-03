package com.pos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PosServerApplication {
	static {
		System.out.println(">>> JVM BOOTED - STARTING POS SERVER...");
	}

	public static void main(String[] args) {
		SpringApplication.run(PosServerApplication.class, args);
	}
}
