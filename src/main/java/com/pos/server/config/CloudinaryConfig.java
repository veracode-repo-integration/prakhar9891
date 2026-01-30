package com.pos.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * DEPRECATED: Cloudinary is no longer used.
 * Switched to Google Cloud Storage (GCS) for production-grade image handling.
 */
@Configuration
public class CloudinaryConfig {
    // Cloudinary bean removed to allow GCS migration
}
