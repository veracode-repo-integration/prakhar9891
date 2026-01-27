package com.pos.server.dto;

import com.pos.server.model.OrderItem;
import com.pos.server.model.User;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CheckoutRequest {
    private List<OrderItem> cart;
    private Map<String, Object> paymentInfo;
    private User user;
}
