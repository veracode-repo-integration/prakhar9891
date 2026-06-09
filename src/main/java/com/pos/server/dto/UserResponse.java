package com.pos.server.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id; // Added ID for API operations
    private String username;
    private String name;
    private String email;
    private String contact;
    private String role;
    private String restaurantId;
    private String onlineStatus;
    private java.util.List<String> allowedFeatures;
    private String restaurantStatus;

    // NO passwordHash field here. IMPOSSIBLE to leak.

    public UserResponse(Long id, String username, String name, String email, String contact, String role,
            String restaurantId, String onlineStatus, java.util.List<String> allowedFeatures, String restaurantStatus) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.role = role;
        this.restaurantId = restaurantId;
        this.onlineStatus = onlineStatus;
        this.allowedFeatures = allowedFeatures;
        this.restaurantStatus = restaurantStatus;
    }

    public UserResponse(Long id, String username, String name, String email, String contact, String role,
            String restaurantId, String onlineStatus, java.util.List<String> allowedFeatures) {
        this(id, username, name, email, contact, role, restaurantId, onlineStatus, allowedFeatures, "ACTIVE");
    }

    public UserResponse(Long id, String username, String name, String email, String contact, String role,
            String restaurantId, String onlineStatus) {
        this(id, username, name, email, contact, role, restaurantId, onlineStatus, new java.util.ArrayList<>(), "ACTIVE");
    }
}
