package com.pos.server.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FcmService {

    public void sendPushNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.info("[FCM] No tokens provided, skipping notification.");
            return;
        }

        log.info("[FCM] Attempting to send notification to {} tokens. Title: {}", tokens.size(), title);

        try {
            // Get named app instance
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance("PosFirebaseApp");
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);

            System.out.println("[FCM] [FORENSIC] DISPATCHING to " + tokens.size() + " tokens on channel online_orders_v13_nuclear_final");
            
            for (String token : tokens) {
                try {
                    Message.Builder messageBuilder = Message.builder()
                            .setToken(token);

                    if (data != null) {
                        messageBuilder.putAllData(data);
                    }
                    
                    // Titan Payload: Force immediate delivery and bypass idle
                    messageBuilder.setAndroidConfig(AndroidConfig.builder()
                            .setTtl(0L) // Deliver NOW
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setChannelId("online_orders_v13_nuclear_final")
                                    .setPriority(AndroidNotification.Priority.MAX)
                                    .setSound("default")
                                    .setVibrateTimingsInMillis(new long[]{0, 500, 250, 500})
                                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                                    .setClickAction("OPEN_URGENT_ORDER")
                                    .build())
                            .build());

                    Message message = messageBuilder.build();
                    String messageId = messaging.send(message);
                    System.out.println("[FCM] [FORENSIC] TITAN SUCCESS for " + token.substring(0, 10) + "... ID: " + messageId);
                } catch (Exception e) {
                    System.err.println("[FCM] [FORENSIC] ERROR for " + token.substring(0, 10) + "... : " + e.getMessage());
                    // Special check for common errors
                    if (e.getMessage() != null && e.getMessage().contains("mismatched-sender-id")) {
                        System.err.println("[FOMIC] SMOKING GUN: Mismatched Sender ID. Check your google-services.json vs Backend Project ID!");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FCM] [FORENSIC] CRITICAL FAILURE: " + e.getMessage());
            log.error("[FCM] Failed to send push notification: {}", e.getMessage());
        }
    }
}
