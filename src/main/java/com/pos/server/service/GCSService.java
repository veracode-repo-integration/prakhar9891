package com.pos.server.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class GCSService {

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.bucket-name:}")
    private String bucketName;

    private Storage storage;

    @PostConstruct
    public void init() throws IOException {
        // Automatically uses application default credentials (ADC) or service account
        // from env
        this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    /**
     * Generates a signed URL for uploading a file directly to GCS.
     * 
     * @param fileName    Name of the file (e.g., product.jpg)
     * @param contentType MIME type (e.g., image/jpeg)
     * @param folder      The folder path (e.g., products, users)
     * @return A map containing the signed upload URL and the final public URL
     */
    public Map<String, String> generateSignedUploadUrl(String fileName, String contentType, String folder) {
        String uniqueFileName = folder + "/" + UUID.randomUUID() + "_" + fileName;
        BlobId blobId = BlobId.of(bucketName, uniqueFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        // Sign URL for a 15-minute window
        URL signedUrl = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(com.google.cloud.storage.HttpMethod.PUT));

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", signedUrl.toString());
        response.put("finalUrl", String.format("https://storage.googleapis.com/%s/%s", bucketName, uniqueFileName));

        return response;
    }

    /**
     * Uploads a base64 image directly to GCS.
     * 
     * @param base64Str The image string (data:image/jpeg;base64,...)
     * @param folder    The folder path (e.g., products)
     * @return The public URL of the uploaded image
     */
    public String uploadBase64Image(String base64Str, String folder) throws IOException {
        if (base64Str == null || !base64Str.startsWith("data:image")) {
            return base64Str;
        }

        // Split "data:image/jpeg;base64,xxxx"
        String[] parts = base64Str.split(",");
        String metadata = parts[0];
        String base64Data = parts[1];

        String contentType = metadata.split(":")[1].split(";")[0];
        String extension = contentType.split("/")[1];
        byte[] bytes = Base64.getDecoder().decode(base64Data);

        String fileName = folder + "/" + UUID.randomUUID().toString() + "." + extension;
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        storage.create(blobInfo, bytes);

        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}
