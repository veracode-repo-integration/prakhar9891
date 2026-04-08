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
                                    .setChannelId("online_orders")
                                    .setPriority(AndroidNotification.Priority.MAX)
                                    .setNotificationPriority(AndroidNotification.Priority.MAX)
                                    .setLightSettings(LightSettings.builder()
                                            .setColor(Color.builder().setRed(1f).setGreen(0f).setBlue(0f).setAlpha(1f).build())
                                            .setLightOnDurationMillis(500)
                                            .setLightOffDurationMillis(500)
                                            .build())
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
            log.info("[FCM] Multicast success: {} failures: {}", response.getSuccessCount(), response.getFailureCount());
            
            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(res -> {
                    if (!res.isSuccessful()) {
                        log.warn("[FCM] Token failure: {}", res.getException().getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.error("[FCM] Failed to send push notification: {}", e.getMessage());
        }
    }
}
