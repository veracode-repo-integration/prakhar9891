package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    private String id; // ORD-000001
    private double total;
    private String method; // cash, card, etc
    private String paymentId;
    private String note;
    private long createdAt;
    private String status; // paid, canceled

    @Column(columnDefinition = "TEXT")
    private String userJson; // Snapshotted user info

    @Column(columnDefinition = "TEXT")
    private String customerJson; // Snapshotted customer info

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;
}
