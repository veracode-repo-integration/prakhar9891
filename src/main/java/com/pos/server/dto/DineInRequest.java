package com.pos.server.dto;

import lombok.Data;

@Data
public class DineInRequest {
    private String tableId;
    private double latitude;
    private double longitude;
}
