package com.pos.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Image Upload Service
 * Supports Cloudinary (if configured) and Local Storage (fallback)
 */
@Service
public class ImageUploadService {

    @Autowired(required = false)
    private Cloudinary cloudinary;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    /**
     * Upload image to Cloudinary or Local Storage
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (isCloudinaryConfigured()) {
            return uploadToCloudinary(file, folder);
        } else {
            return uploadLocally(file);
        }
    }

    private boolean isCloudinaryConfigured() {
        return cloudinary != null && cloudinaryApiKey != null && !cloudinaryApiKey.isEmpty()
                && !cloudinaryApiKey.startsWith("YOUR_");
    }

    private String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto",
                        "transformation", new com.cloudinary.Transformation()
                                .width(800)
                                .height(800)
                                .crop("limit")
                                .quality("auto")
                                .fetchFormat("auto")));
        return (String) uploadResult.get("secure_url");
    }

    private String uploadLocally(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_"
                + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Generate full URL based on current request context
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
    }

    /**
     * Delete image
     */
    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl == null)
            return;

        if (imageUrl.contains("cloudinary.com")) {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null && cloudinary != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } else if (imageUrl.contains("/uploads/")) {
            // Local file deletion logic could go here if needed
            // identifying file name from URL is tricky without strict format, skipping for
            // safety
        }
    }

    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }
        try {
            String[] parts = imageUrl.split("/");
            String fileWithExt = parts[parts.length - 1];
            return fileWithExt.contains(".") ? fileWithExt.substring(0, fileWithExt.lastIndexOf('.')) : fileWithExt;
        } catch (Exception e) {
            return null;
        }
    }
}
