package com.pos.server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    @org.springframework.beans.factory.annotation.Value("${pos.jwt.secret:pos_system_ultra_secure_secret_key_1234567890}")
    private String jwtSecret;

    @org.springframework.beans.factory.annotation.Value("${pos.jwt.expiration:604800000}") // Default 7 days
    private long expirationMs;

    private Key getSigningKey() {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            byte[] keyBytes = new byte[32];
            byte[] src = jwtSecret.getBytes();
            System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(String username, String role, String restaurantId) {
        return generateToken(username, role, restaurantId, null);
    }

    public String generateToken(String username, String role, String restaurantId, String tableId) {
        long currentExpiration = expirationMs;
        if ("GUEST".equalsIgnoreCase(role)) {
            currentExpiration = 10800000; // 3 hours (3 * 60 * 60 * 1000)
        }
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("restaurantId", restaurantId);
        if (tableId != null) {
            builder.claim("tableId", tableId);
        }
        return builder.setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + currentExpiration))
                .signWith(getSigningKey())
                .compact();
    }


    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRoleFromToken(String token) {
        try {
            Object role = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().get("role");
            return role != null ? role.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getRestaurantIdFromToken(String token) {
        try {
            Object rid = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().get("restaurantId");
            return rid != null ? rid.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getTableIdFromToken(String token) {
        try {
            Object tid = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().get("tableId");
            return tid != null ? tid.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirationTimeFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().getExpiration().getTime();
        } catch (Exception e) {
            return System.currentTimeMillis() + expirationMs;
        }
    }


    @Autowired
    private com.pos.server.service.TokenBlacklistService tokenBlacklistService;

    public boolean validateToken(String token) {
        try {
            if (tokenBlacklistService.isBlacklisted(token)) {
                return false;
            }
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
