package com.pos.server.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe rate limiter to protect backend against DDoS and request flooding.
 * Limits client IPs to a maximum of 120 requests per rolling minute.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private final ConcurrentHashMap<String, RequestTracker> ipRequestMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        // Allow pre-flight OPTIONS requests without tracking
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestTracker tracker = ipRequestMap.compute(ip, (key, currentTracker) -> {
            if (currentTracker == null || now - currentTracker.windowStart > 60000) {
                return new RequestTracker(now, 1);
            } else {
                currentTracker.count.incrementAndGet();
                return currentTracker;
            }
        });

        if (tracker.count.get() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"ok\": false, \"message\": \"Too many requests. Please slow down.\"}");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class RequestTracker {
        final long windowStart;
        final AtomicInteger count;

        RequestTracker(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
