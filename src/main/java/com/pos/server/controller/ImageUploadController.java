package com.pos.server.controller;

import org.springframework.web.bind.annotation.*;

/*
 * DEPRECATED: Use StorageController for GCS Signed URL flow.
 * This class is kept temporarily to avoid compilation issues but is no longer functional.
 */
@RestController
@RequestMapping("/api/deprecated")
@CrossOrigin(origins = "*")
public class ImageUploadController {
    // Methods removed to comply with GCS-only architecture
}
