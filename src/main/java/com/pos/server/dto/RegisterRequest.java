package com.pos.server.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password; // The raw password from frontend
    private String firstName;
    private String lastName;
    private String email;
    private String contact;
    private String role;
}
