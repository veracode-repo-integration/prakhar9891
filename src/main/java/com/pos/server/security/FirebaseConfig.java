package com.pos.server.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    
    @Value("${firebase.project-id:udhyog-khaata}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            String appName = "PosFirebaseApp";
            boolean appExists = FirebaseApp.getApps().stream()
                    .anyMatch(app -> app.getName().equals(appName));

            if (!appExists) {
                GoogleCredentials credentials;
                java.io.File file = new java.io.File("service-account.json");
                
                if (file.exists()) {
                    System.out.println("[Firebase] Found service-account.json in root. Loading credentials...");
                    credentials = GoogleCredentials.fromStream(new java.io.FileInputStream(file));
                } else {
                    System.out.println("[Firebase] service-account.json not found in root. Using Application Default.");
                    credentials = GoogleCredentials.getApplicationDefault();
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options, appName);
                System.out.println("[FORENSIC] Google Firebase App '" + appName + "' Initialized successfully.");
            }
        } catch (IOException e) {
            System.err.println("[Firebase] Failed to initialize named app: " + e.getMessage());
        }
    }
}
