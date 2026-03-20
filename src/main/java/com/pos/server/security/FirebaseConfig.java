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
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options, appName);
                System.out.println("[Firebase] Initialized named app '" + appName + "' with Project ID: " + projectId);
            }
        } catch (IOException e) {
            System.err.println("[Firebase] Failed to initialize named app: " + e.getMessage());
        }
    }
}
