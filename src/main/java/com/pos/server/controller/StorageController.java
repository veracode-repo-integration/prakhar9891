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

    /**
     * Request a signed URL for file upload.
     * Expected params: fileName, contentType, folder (default 'general')
     */
    @GetMapping("/signed-url")
    public ResponseEntity<?> getSignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(defaultValue = "general") String folder) {
        
        try {
            Map<String, String> response = gcsService.generateSignedUploadUrl(fileName, contentType, folder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
