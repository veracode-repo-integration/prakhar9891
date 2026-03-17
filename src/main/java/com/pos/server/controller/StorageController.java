package com.pos.server.controller;

import com.pos.server.service.GCSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@CrossOrigin(origins = "*")
public class StorageController {

    @Autowired
    private GCSService gcsService;

    @Autowired
    private com.pos.server.service.AuthService authService;

    /**
     * Request a signed URL for file upload.
     * Expected params: fileName, contentType, folder (default 'general')
     */
    @GetMapping("/signed-url")
    public ResponseEntity<?> getSignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(defaultValue = "general") String folder,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {

        try {
            authService.validateRestaurantAccess(restaurantId);
            Map<String, String> response = gcsService.generateSignedUploadUrl(fileName, contentType, folder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Upload a file via base64 directly to the server, then to GCS.
     * Use this if signed-url flow fails or is restricted.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadBase64(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            authService.validateRestaurantAccess(restaurantId);
            String base64 = payload.get("base64");
            String folder = payload.getOrDefault("folder", "general");

            if (base64 == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Base64 data is required"));
            }

            String publicUrl = gcsService.uploadBase64Image(base64, folder);
            return ResponseEntity.ok(Map.of("url", publicUrl));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
