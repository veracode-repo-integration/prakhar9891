package com.pos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		SecurityAutoConfiguration.class
})
@RestController
public class PosServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosServerApplication.class, args);
	}

	@GetMapping("/")
	public String health() {
		return "🚀 INFRASTRUCTURE VERIFIED - SERVER IS RUNNING!";
	}
}
