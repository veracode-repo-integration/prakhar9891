package com.pos.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class PosServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosServerApplication.class, args);
	}

	@Bean
	public CommandLineRunner debugDatabase(DataSource dataSource) {
		return args -> {
			System.out.println("🔍 STARTING DATABASE CONNECTION TEST...");
			try (Connection conn = dataSource.getConnection()) {
				System.out.println(
						"✅ DATABASE CONNECTION SUCCESSFUL! DB: " + conn.getMetaData().getDatabaseProductName());
			} catch (Exception e) {
				System.err.println("❌ DATABASE CONNECTION FAILED: " + e.getMessage());
				e.printStackTrace();
			}
		};
	}
}
