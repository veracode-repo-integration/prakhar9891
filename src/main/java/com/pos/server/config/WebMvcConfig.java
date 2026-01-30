package com.pos.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration to serve uploaded files locally
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from the 'uploads' directory at '/uploads/**'
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
