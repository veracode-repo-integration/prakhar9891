package com.pos.server.service;

import com.pos.server.model.Order;
import com.pos.server.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * THE ATOMIC HAMMER
 * This service manages continuous high-priority siren pulses for pending online orders.
 * It ensures the phone keeps ringing every 15 seconds until the user acts.
 */
@Service
@Slf4j
public class SirenSchedulerService {

    @Autowired
    @Lazy
    private FcmService fcmService;

    @Autowired
    @Lazy
    private AuthService authService;

    @Autowired
    private OrderRepository orderRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> activeSirens = new ConcurrentHashMap<>();

    private static final int PULSE_INTERVAL_SECONDS = 15;
    private static final int MAX_DURATION_MINUTES = 5;

    public void startSiren(Order order) {
        if (order == null || order.getId() == null) return;
        
        String orderId = order.getId();
        if (activeSirens.containsKey(orderId)) {
            log.info("[SirenHammer] Siren already active for order: {}", orderId);
            return;
        }

        log.info("[SirenHammer] STARTING CONTINUOUS SIREN for Order: {}", orderId);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                pulse(orderId);
            } catch (Exception e) {
                log.error("[SirenHammer] Pulse failed for {}: {}", orderId, e.getMessage());
            }
        }, 0, PULSE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        activeSirens.put(orderId, future);

        // Auto-kill after timeout
        scheduler.schedule(() -> stopSiren(orderId, "Timeout"), MAX_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    private void pulse(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isEmpty()) {
            stopSiren(orderId, "Order missing from DB");
            return;
        }

        Order order = orderOpt.get();
        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        String kStatus = order.getKitchenStatus() != null ? order.getKitchenStatus().toLowerCase() : "";

        // STOP if order is no longer pending
        if (!status.equals("pending") && !kStatus.equals("pending")) {
            stopSiren(orderId, "Status changed to " + status + "/" + kStatus);
            return;
        }

        // DISPATCH PULSE
        String rid = order.getRestaurantId();
        java.util.List<String> tokens = authService.findTokensByRestaurantIdSmart(rid);

        if (!tokens.isEmpty()) {
            log.info("[SirenHammer] PULSING siren to {} devices for order {}", tokens.size(), orderId);
            String title = "!!! PENDING ONLINE ORDER !!!";
            String body = "Order #" + orderId + " is still waiting for response!";
            
            Map<String, String> data = Map.of(
                "type", "NEW_ONLINE_ORDER",
                "orderId", orderId,
                "restaurantId", rid != null ? rid : "",
                "isPulse", "true"
            );

            fcmService.dispatchHighPrioritySiren(tokens, title, body, data);
        } else {
            log.warn("[SirenHammer] Pulse aborted: No tokens for RID {}", rid);
        }
    }

    public void stopSiren(String orderId, String reason) {
        ScheduledFuture<?> future = activeSirens.remove(orderId);
        if (future != null) {
            future.cancel(false);
            log.info("[SirenHammer] STOPPED siren for {}. Reason: {}", orderId, reason);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SirenHammer] Shutting down scheduler...");
        scheduler.shutdownNow();
    }
}
