package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pos_audit_logs", indexes = {
        @Index(name = "idx_audit_restaurant_time", columnList = "restaurant_id, created_at")
})
@org.hibernate.annotations.BatchSize(size = 50)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "action", nullable = false)
    private String action; // VOID_ORDER, VOID_TABLE, MANUAL_DISCOUNT, DRAWER_KICKOUT, PRICE_OVERRIDE

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "manager_authorized")
    private Boolean managerAuthorized = false;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
