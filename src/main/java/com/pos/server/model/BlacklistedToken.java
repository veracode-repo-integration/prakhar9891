package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "blacklisted_tokens", indexes = {
        @Index(name = "idx_blacklist_token", columnList = "token")
})
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String token;

    @Column(name = "expiry_time")
    private long expiryTime;
}
