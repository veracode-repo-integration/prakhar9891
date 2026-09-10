package com.pos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.pos.server.repository.UserRepository;
import com.pos.server.repository.RestaurantRepository;

@SpringBootApplication
public class PosServerApplication {
	static {
		System.out.println(">>> JVM BOOTED - STARTING POS SERVER...");
	}

	public static void main(String[] args) {
		System.out.println(">>> STARTING SPRING CONTEXT...");
		SpringApplication.run(PosServerApplication.class, args);
		System.out.println(">>> SPRING CONTEXT STARTED SUCCESSFULLY.");
	}

	@Bean
	public org.springframework.boot.CommandLineRunner clearExistingProductImages(com.pos.server.repository.ProductRepository productRepository) {
		return args -> {
			try {
				int cleared = productRepository.clearAllProductImagesInDb();
				System.out.println(">>> Successfully cleared " + cleared + " existing product images from the database.");
			} catch (Exception e) {
				System.err.println(">>> Note on clearing images at startup: " + e.getMessage());
			}
		};
	}
}
