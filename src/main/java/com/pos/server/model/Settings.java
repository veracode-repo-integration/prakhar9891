package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "settings")
public class Settings {
    @Id
    private String id = "default"; // Singleton

    private double taxRate;
    private String upiId;
    private String merchantName;
    private String gstNumber;
    private String address;
    private String footerMessage;

    // Store Location for Geofencing
    private double latitude;
    private double longitude;

    @Column(columnDefinition = "TEXT")
    private String tableAreas; // Comma-separated or JSON string of area names
}
