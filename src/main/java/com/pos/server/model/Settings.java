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

    @Column(name = "state_code")
    private String stateCode = "27"; // Default MH

    @Column(name = "state_name")
    private String stateName = "Maharashtra";

    @Column(name = "gst_category")
    private String gstCategory = "Regular"; // Regular, Composition, Unregistered

    @Column(name = "invoice_prefix")
    private String invoicePrefix = "INV-2025-";

    @Column(columnDefinition = "TEXT")
    private String rawSettingsJson;
}
