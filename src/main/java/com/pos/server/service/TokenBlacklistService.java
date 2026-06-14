package com.pos.server.service;

import com.pos.server.model.BlacklistedToken;
import com.pos.server.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository repository;

    public void blacklistToken(String token, long expiryTime) {
        if (token != null && !token.trim().isEmpty() && !repository.existsByToken(token)) {
            BlacklistedToken bt = new BlacklistedToken();
            bt.setToken(token);
            bt.setExpiryTime(expiryTime);
            repository.save(bt);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return repository.existsByToken(token);
    }

    /**
     * Clean up expired tokens once every day.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        repository.findAll().stream()
                .filter(t -> t.getExpiryTime() < System.currentTimeMillis())
                .forEach(t -> repository.delete(t));
    }
}
