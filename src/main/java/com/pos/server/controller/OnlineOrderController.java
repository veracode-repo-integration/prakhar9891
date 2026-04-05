package com.pos.server.controller;

import com.pos.server.model.Order;
import com.pos.server.model.OrderItem;
import com.pos.server.model.Product;
import com.pos.server.model.Restaurant;
import com.pos.server.service.AuthService;
import com.pos.server.service.OrderService;
import com.pos.server.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/webhooks")
public class OnlineOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Unified Webhook for Zomato, Swiggy, etc. via Aggregator (UrbanPiper style)
     */
    @PostMapping("/orders")
    public ResponseEntity<?> receiveOnlineOrder(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Restaurant-Id", required = true) String restaurantId) {
        
        try {
            // 1. Extract Basic Info
            Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
            if (orderData == null) orderData = payload; // Fallback if not nested

            String externalId = (String) orderData.get("id");
            String platform = (String) orderData.get("platform"); // 'ZOMATO', 'SWIGGY'
            
            // 2. Initialize POS Order
            Order posOrder = new Order();
            posOrder.setId("ONL-" + externalId);
            posOrder.setExternalId(externalId);
            posOrder.setAggregatorName(platform);
            posOrder.setIsOnline(true);
            posOrder.setRestaurantId(restaurantId);
            posOrder.setOrderType("takeaway");
            posOrder.setKitchenStatus("pending");
            posOrder.setStatus("pending");
            posOrder.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));

            // 3. Normalize Items
            List<Map<String, Object>> externalItems = (List<Map<String, Object>>) orderData.get("items");
            List<OrderItem> localItems = new ArrayList<>();
            double subtotal = 0;

            for (Map<String, Object> eItem : externalItems) {
                Object rawId = eItem.get("id");
                Object rawQty = eItem.get("quantity");
                
                if (rawId == null || rawQty == null) continue;
                
                String eProductId = String.valueOf(rawId);
                int qty = (rawQty instanceof Number) ? ((Number) rawQty).intValue() : Integer.parseInt(String.valueOf(rawQty));
                
                // Find local product by mapping
                Product product = findProductByMapping(restaurantId, platform, eProductId);
                if (product != null) {
                    // Use mapped price if available, else standard price
                    double finalPrice = product.getPrice();
                    try {
                        Map<String, Object> mappings = objectMapper.readValue(product.getExternalMappings(), Map.class);
                        Object mp = mappings.get(platform);
                        if (mp instanceof Map) {
                            Object priceObj = ((Map) mp).get("price");
                            if (priceObj != null) {
                                finalPrice = Double.parseDouble(String.valueOf(priceObj));
                            }
                        }
                    } catch (Exception e) {}

                    OrderItem localItem = new OrderItem();
                    localItem.setItemId(UUID.randomUUID().toString());
                    localItem.setProductId(product.getId());
                    localItem.setName(product.getName());
                    localItem.setPrice(finalPrice);
                    localItem.setQty(qty);
                    
                    // --- SNAPSHOT HSN & TAX FOR AUDIT ---
                    localItem.setHsnCode(product.getHsnCode() != null ? product.getHsnCode() : "9963");
                    localItem.setTaxCategory(product.getTaxCategory());
                    
                    // Default to 5% if unspecified (Standard for restaurants)
                    double taxRate = product.getTax() != null ? product.getTax() : 5.0;
                    double itemTotal = finalPrice * qty;
                    double taxAmount = (itemTotal * taxRate) / 100.0;
                    
                    localItem.setTaxRate(taxRate);
                    localItem.setTaxAmount(taxAmount);
                    localItem.setCgstAmount(taxAmount / 2.0);
                    localItem.setSgstAmount(taxAmount / 2.0);
                    localItem.setTaxableValue(itemTotal);

                    localItem.setOrder(posOrder);
                    localItems.add(localItem);
                    subtotal += itemTotal;
                }
            }

            posOrder.setItems(localItems);
            posOrder.setSubtotal(subtotal);
            
            // Calculate total tax for the entire order
            double totalTax = localItems.stream().mapToDouble(i -> i.getTaxAmount()).sum();
            posOrder.setTaxAmount(totalTax);
            posOrder.setCgstAmount(totalTax / 2.0);
            posOrder.setSgstAmount(totalTax / 2.0);
            posOrder.setTotal(subtotal + totalTax);
            
            // 4. Save & Notify
            Order saved = orderService.saveOrderDirectly(posOrder);
            
            messagingTemplate.convertAndSend("/topic/orders", Map.of(
                    "order", saved,
                    "type", "NEW_ONLINE_ORDER",
                    "actingUser", "aggregator"
            ));

            return ResponseEntity.ok(Map.of("status", "success", "pos_order_id", saved.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private Product findProductByMapping(String restaurantId, String platform, String externalId) {
        List<Product> allProducts = productService.getAllProductsNoCache(restaurantId);
        for (Product p : allProducts) {
            if (p.getExternalMappings() == null) continue;
            try {
                Map<String, Object> mappings = objectMapper.readValue(p.getExternalMappings(), Map.class);
                Object platformMapping = mappings.get(platform);
                
                if (platformMapping instanceof Map) {
                    Map<String, Object> mapObj = (Map<String, Object>) platformMapping;
                    if (externalId.equals(String.valueOf(mapObj.get("id")))) {
                        return p;
                    }
                } else if (platformMapping != null) {
                    if (externalId.equals(String.valueOf(platformMapping))) {
                        return p;
                    }
                }
            } catch (Exception e) { }
        }
        return null;
    }
}
