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

    // NO passwordHash field here. IMPOSSIBLE to leak.

    public UserResponse(Long id, String username, String name, String email, String contact, String role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.role = role;
    }
}
