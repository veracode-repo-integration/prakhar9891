package com.pos.server.dto;

import com.pos.server.model.OrderItem;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CheckoutRequest {
    private List<OrderItem> items;
    private Map<String, Object> paymentInfo;
    private Object user; // Can be String or Map

    // New fields for full compatibility
    private Map<String, Object> discount; // { type: 'amount', value: 10 }
    private double taxRate;
    private double taxAmount;
    private double total; // Frontend calculated total including tax/discount
    private String orderType; // 'dine-in', 'takeaway'
    private String tableId;
    private String status; // 'pending', 'paid'
    private boolean payLater;
    private double payLaterAmount;
    private int pointsEarned;
    private String kitchenStatus; // 'pending', 'ready'
    private String restaurantId;
}
