package com.pos.server.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket(String path) {
        if (path != null && (path.contains("/api/auth/login") || path.contains("/api/auth/register"))) {
            // Strong rate limit for sensitive authentication routes
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                    .build();
        }
        // General limit for regular CRUD APIs
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (path.startsWith("/api")) {
            String ip = getClientIP(request);
            String cacheKey = ip + ":" + (path.contains("/api/auth/") ? "auth" : "gen");
            
            Bucket bucket = cache.computeIfAbsent(cacheKey, k -> createNewBucket(path));

            if (!bucket.tryConsume(1)) {
                System.err.println("[SECURITY] [RATE-LIMIT] Blocked request from IP: " + ip + " for path: " + path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
