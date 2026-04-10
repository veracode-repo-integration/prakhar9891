package com.pos.server.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FcmService {

    public void dispatchHighPrioritySiren(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;
        
        System.out.println("[FCM] [ELITE-SIREN] Dispatching to " + tokens.size() + " tokens on Channel v17 (NUCLEAR)");
        
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance("PosFirebaseApp");
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);

            for (String token : tokens) {
                try {
                    Message message = Message.builder()
                            .setToken(token)
                            .putAllData(data != null ? data : Map.of())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setTtl(0L) // Instant delivery or expire
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setDirectBootOk(true) // Crucial for locked devices
                                    .setNotification(AndroidNotification.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .setChannelId("online_orders_v17_nuclear")
                                            .setPriority(AndroidNotification.Priority.MAX)
                                            .setSound("siren")
                                            .setDefaultSound(false)
                                            .setNotificationCount(1)
                                            .setVisibility(AndroidNotification.Visibility.PUBLIC)
                                            .setClickAction("OPEN_URGENT_ORDER")
                                            .build())
                                    .build())
                            .build();

                    String responseId = messaging.send(message);
                    System.out.println("[FCM] [ELITE-SUCCESS] Message sent successfully. ID: " + responseId);
                } catch (Exception e) {
                    System.err.println("[FCM] [ELITE-ERR] Token: " + (token != null && token.length() > 5 ? token.substring(0, 5) : "INVALID") + "... : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[FCM] Critical Dispatch Failure: {}", e.getMessage());
        }
    }

    public void sendPushNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        dispatchHighPrioritySiren(tokens, title, body, data);
    }

}
