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

	@Bean
	public org.springframework.boot.CommandLineRunner cleanupUser(
			UserRepository userRepository,
			RestaurantRepository restaurantRepository) {
		return args -> {
			System.out.println(">>> RUNNING STARTUP CLEANUP: Searching for user 9891260801 in divya...");
			userRepository.findAll().stream()
				.filter(u -> "divya".equals(u.getRestaurantId()) && u.getContact() != null && u.getContact().contains("9891260801"))
				.forEach(u -> {
					System.out.println(">>> DELETING CONFLICTING USER: " + u.getUsername() + " from restaurant: " + u.getRestaurantId());
					userRepository.delete(u);
				});

			System.out.println(">>> RUNNING STARTUP CLEANUP: Activating restaurant divya...");
			restaurantRepository.findById("divya").ifPresent(r -> {
				if (!"ACTIVE".equals(r.getStatus())) {
					System.out.println(">>> ACTIVATING RESTAURANT: " + r.getId());
					r.setStatus("ACTIVE");
					restaurantRepository.save(r);
				}
			});
			System.out.println(">>> STARTUP CLEANUP COMPLETE.");
		};
	}

	public static void main(String[] args) {
		System.out.println(">>> STARTING SPRING CONTEXT...");
		SpringApplication.run(PosServerApplication.class, args);
		System.out.println(">>> SPRING CONTEXT STARTED SUCCESSFULLY.");
	}
}
