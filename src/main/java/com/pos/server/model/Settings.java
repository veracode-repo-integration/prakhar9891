package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_terminal_config")
public class Settings {
    @Id
    private String id = "default";

    private Double taxRate;
    private String upiId;
    private String merchantName;
    private String gstNumber;
    private String address;
    private String footerMessage;

    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String tableAreas;

    private Boolean skipCustomerDetails;
    private Boolean notifications;

    @Column(columnDefinition = "TEXT")
    private String taxMapping;

    private Boolean taxInclusive;

    @Column(name = "restaurant_id")
    private String restaurantId;

    @Column(columnDefinition = "TEXT")
    private String rawSettingsJson;
}
