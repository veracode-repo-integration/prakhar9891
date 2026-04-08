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

            System.out.println("[FCM] Executing Multicast for " + tokens.size() + " devices. Channel: online_orders_v3");

            // Multicast support up to 500 tokens
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("siren") // siren.mp3 in res/raw
                                    .setChannelId("online_orders_v3")
                                    .setPriority(AndroidNotification.Priority.MAX)
                                    .setVibrateTimingsInMillis(new long[]{0, 500, 250, 500})
                                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                                    .setSticky(true)
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("siren.aiff")
                                    .setCategory("ONLINE_ORDER")
                                    .build())
                            .build())
                    .putAllData(data)
                    .build();

            BatchResponse response = messaging.sendMulticast(message);
            System.out.println("[FCM] Multicast Finished. Result: " + response.getSuccessCount() + " SUCCESS, " + response.getFailureCount() + " FAILURE.");
            
            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(res -> {
                    if (!res.isSuccessful()) {
                        System.err.println("[FCM] Token Failure Reason: " + res.getException().getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[FCM] CRITICAL SEND FAILURE: " + e.getMessage());
            log.error("[FCM] Failed to send push notification: {}", e.getMessage());
        }
    }
}
