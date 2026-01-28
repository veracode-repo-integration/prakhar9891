package com.pos.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Image Upload Service using Cloudinary
 * Handles product image uploads to cloud storage for optimal performance
 */
@Service
public class ImageUploadService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload image to Cloudinary and return the URL
     * 
     * @param file   Image file to upload
     * @param folder Folder name in Cloudinary (e.g., "products")
     * @return Image URL from Cloudinary CDN
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
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

    /**
     * Delete image from Cloudinary
     * 
     * @param publicId Public ID of the image to delete
     */
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Extract public ID from Cloudinary URL
     * 
     * @param imageUrl Full Cloudinary URL
     * @return Public ID
     */
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }
        String[] parts = imageUrl.split("/");
        String fileWithExt = parts[parts.length - 1];
        return fileWithExt.substring(0, fileWithExt.lastIndexOf('.'));
    }
}
