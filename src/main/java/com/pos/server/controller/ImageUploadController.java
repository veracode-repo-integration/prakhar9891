package com.pos.server.controller;

import com.pos.server.service.ImageUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Image Upload Controller
 * Handles product image uploads to Cloudinary for optimal performance
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageUploadController {

    @Autowired
    private ImageUploadService imageUploadService;

    /**
     * Upload product image
     * POST /api/upload-image
     * 
     * @param file Image file (multipart/form-data)
     * @return JSON with image URL
     */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size must be less than 5MB"));
            }

            // Upload to Cloudinary
            String imageUrl = imageUploadService.uploadImage(file, "pos-products");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "Image uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Delete product image
     * DELETE /api/delete-image
     * 
     * @param imageUrl Full Cloudinary URL
     * @return Success response
     */
    @DeleteMapping("/delete-image")
    public ResponseEntity<?> deleteProductImage(@RequestParam("imageUrl") String imageUrl) {
        try {
            String publicId = imageUploadService.extractPublicId(imageUrl);
            if (publicId != null) {
                imageUploadService.deleteImage(publicId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to delete image: " + e.getMessage()));
        }
    }
}
