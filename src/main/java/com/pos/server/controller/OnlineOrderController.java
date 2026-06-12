package com.pos.server.controller;

import com.pos.server.model.Order;
import com.pos.server.model.OrderItem;
import com.pos.server.model.Product;
import com.pos.server.model.Restaurant;
import com.pos.server.model.Settings;
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
    private AuthService authService;

    @Autowired
    private com.pos.server.service.FcmService fcmService;

    @Autowired
    private com.pos.server.repository.UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.pos.server.service.SirenSchedulerService sirenSchedulerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.pos.server.service.CommonDataService commonDataService;

    /**
     * Pull menu API for online aggregators (e.g. Ghar Tak)
     */
    @GetMapping("/menu")
    public ResponseEntity<?> getMenuFeed(
            @RequestParam("platform") String platform,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {

        System.out.println("[FORENSIC] [MENU-PULL] RID: " + restaurantId + " | PLATFORM: " + platform);

        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("X-Restaurant-Id header is required");
        }

        try {
            // 1. Get Platform Markup from Settings
            Settings settings = commonDataService.getSettings(restaurantId);
            double markupPercent = 0.0;
            if (settings.getRawSettingsJson() != null && !settings.getRawSettingsJson().trim().isEmpty()) {
                try {
                    Map<String, Object> rawMap = objectMapper.readValue(settings.getRawSettingsJson(), Map.class);
                    Object markupsObj = rawMap.get("platformMarkups");
                    if (markupsObj instanceof Map) {
                        Map<String, Object> markups = (Map<String, Object>) markupsObj;
                        Object m = null;
                        for (Map.Entry<String, Object> entry : markups.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(platform)) {
                                m = entry.getValue();
                                break;
                            }
                        }
                        if (m != null) {
                            markupPercent = Double.parseDouble(String.valueOf(m));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[FORENSIC] Failed to read platform markup: " + e.getMessage());
                }
            }

            // 2. Fetch all products
            List<Product> products = productService.getAllProductsNoCache(restaurantId);
            List<Map<String, Object>> responseList = new ArrayList<>();
            String pUpper = (platform != null ? platform.trim().toUpperCase() : "");
            String prefix;
            if ("GHARTAK".equals(pUpper)) {
                prefix = "gt";
            } else if ("SWIGGY".equals(pUpper)) {
                prefix = "sw";
            } else if ("ZOMATO".equals(pUpper)) {
                prefix = "zo";
            } else {
                prefix = platform.substring(0, Math.min(2, platform.length())).toLowerCase();
            }

            for (Product p : products) {
                // Calculate Online ID (custom mapping or fallback)
                String onlineId = null;
                double onlinePrice = p.getPrice();
                boolean hasCustomPrice = false;

                if (p.getExternalMappings() != null) {
                    try {
                        Map<String, Object> mappings = objectMapper.readValue(p.getExternalMappings(), Map.class);
                        Object pmObj = mappings.get(platform);
                        if (pmObj instanceof Map) {
                            Map<String, Object> pmMap = (Map<String, Object>) pmObj;
                            if (pmMap.get("id") != null) {
                                onlineId = String.valueOf(pmMap.get("id"));
                            }
                            if (pmMap.get("price") != null) {
                                onlinePrice = Double.parseDouble(String.valueOf(pmMap.get("price")));
                                hasCustomPrice = true;
                            }
                        } else if (pmObj != null) {
                            onlineId = String.valueOf(pmObj);
                        }
                    } catch (Exception e) {}
                }

                if (onlineId == null) {
                    onlineId = prefix + "_" + p.getName().replaceAll("\\s+", "").toLowerCase();
                }

                if (!hasCustomPrice && markupPercent > 0.0) {
                    onlinePrice = p.getPrice() * (1 + markupPercent / 100.0);
                }

                // Determine stock availability
                boolean inStock = true;
                if ("out_of_stock".equalsIgnoreCase(p.getOnlineAvailability())) {
                    inStock = false;
                } else if (p.getStock() != null && p.getStock() <= 0) {
                    inStock = false;
                }

                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("id", onlineId);
                itemMap.put("name", p.getName());
                itemMap.put("price", onlinePrice);
                itemMap.put("base_price", p.getPrice());
                itemMap.put("category", p.getCategory());
                itemMap.put("in_stock", inStock);
                itemMap.put("image", p.getImage());
                itemMap.put("tax_rate", p.getTax() != null ? p.getTax() : 5.0);
                itemMap.put("hsn_code", p.getHsnCode() != null ? p.getHsnCode() : "9963");

                responseList.add(itemMap);
            }

            return ResponseEntity.ok(responseList);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Unified Webhook for Zomato, Swiggy, etc. via Aggregator (UrbanPiper style)
     */
    @PostMapping("/orders")
    public ResponseEntity<?> receiveOnlineOrder(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {

        System.out.println("[FORENSIC] [WEBHOOK-HIT] RID_HEADER: " + restaurantId + " | PAYLOAD: "
                + (payload != null ? "YES" : "NO"));

        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            System.err.println("[FORENSIC] [WEBHOOK-ERR] REJECTED: X-Restaurant-Id header is MISSING!");
            return ResponseEntity.badRequest().body("X-Restaurant-Id header is required");
        }

        try {
            // 1. Extract Basic Info
            Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
            if (orderData == null)
                orderData = payload; // Fallback if not nested

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

                if (rawId == null || rawQty == null)
                    continue;

                String eProductId = String.valueOf(rawId);
                int qty = (rawQty instanceof Number) ? ((Number) rawQty).intValue()
                        : Integer.parseInt(String.valueOf(rawQty));

                // Find local product by mapping
                Product product = findProductByMapping(restaurantId, platform, eProductId);
                if (product != null) {
                    // Use mapped price if available, else standard price
                    double finalPrice = product.getPrice();
                    boolean hasCustomPrice = false;
                    try {
                        if (product.getExternalMappings() != null) {
                            Map<String, Object> mappings = objectMapper.readValue(product.getExternalMappings(), Map.class);
                            Object mp = null;
                            for (Map.Entry<String, Object> entry : mappings.entrySet()) {
                                if (entry.getKey().equalsIgnoreCase(platform)) {
                                    mp = entry.getValue();
                                    break;
                                }
                            }
                            if (mp instanceof Map) {
                                Object priceObj = ((Map) mp).get("price");
                                if (priceObj != null) {
                                    finalPrice = Double.parseDouble(String.valueOf(priceObj));
                                    hasCustomPrice = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                    }

                    if (!hasCustomPrice) {
                        try {
                            Settings settings = commonDataService.getSettings(restaurantId);
                            if (settings.getRawSettingsJson() != null && !settings.getRawSettingsJson().trim().isEmpty()) {
                                Map<String, Object> rawMap = objectMapper.readValue(settings.getRawSettingsJson(), Map.class);
                                Object markupsObj = rawMap.get("platformMarkups");
                                if (markupsObj instanceof Map) {
                                    Map<String, Object> markups = (Map<String, Object>) markupsObj;
                                    Object m = null;
                                    for (Map.Entry<String, Object> entry : markups.entrySet()) {
                                        if (entry.getKey().equalsIgnoreCase(platform)) {
                                            m = entry.getValue();
                                            break;
                                        }
                                    }
                                    if (m != null) {
                                        double markupPercent = Double.parseDouble(String.valueOf(m));
                                        if (markupPercent > 0.0) {
                                            finalPrice = product.getPrice() * (1 + markupPercent / 100.0);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[FORENSIC] Failed to read platform markup in webhook: " + e.getMessage());
                        }
                    }

                    OrderItem localItem = new OrderItem();
                    localItem.setItemId(UUID.randomUUID().toString());
                    localItem.setProductId(product.getId());
                    localItem.setName(product.getName());
                    finalPrice = Math.round(finalPrice * 100.0) / 100.0;
                    localItem.setPrice(finalPrice);
                    localItem.setQty(qty);

                    // --- SNAPSHOT HSN & TAX FOR AUDIT ---
                    localItem.setHsnCode(product.getHsnCode() != null ? product.getHsnCode() : "9963");
                    localItem.setTaxCategory(product.getTaxCategory());

                    // Default to 5% if unspecified (Standard for restaurants)
                    double taxRate = product.getTax() != null ? product.getTax() : 5.0;
                    double itemTotal = Math.round((finalPrice * qty) * 100.0) / 100.0;
                    double taxAmount = Math.round(((itemTotal * taxRate) / 100.0) * 100.0) / 100.0;

                    localItem.setTaxRate(taxRate);
                    localItem.setTaxAmount(taxAmount);
                    localItem.setCgstAmount(Math.round((taxAmount / 2.0) * 100.0) / 100.0);
                    localItem.setSgstAmount(Math.round((taxAmount / 2.0) * 100.0) / 100.0);
                    localItem.setTaxableValue(itemTotal);

                    localItem.setOrder(posOrder);
                    localItems.add(localItem);
                    subtotal += itemTotal;
                }
            }

            posOrder.setItems(localItems);
            double roundedSubtotal = Math.round(subtotal * 100.0) / 100.0;
            posOrder.setSubtotal(roundedSubtotal);

            // Calculate total tax for the entire order
            double totalTax = localItems.stream().mapToDouble(i -> i.getTaxAmount()).sum();
            double roundedTax = Math.round(totalTax * 100.0) / 100.0;
            posOrder.setTaxAmount(roundedTax);
            posOrder.setCgstAmount(Math.round((roundedTax / 2.0) * 100.0) / 100.0);
            posOrder.setSgstAmount(Math.round((roundedTax / 2.0) * 100.0) / 100.0);
            
            double roundedTotal = Math.round((roundedSubtotal + roundedTax) * 100.0) / 100.0;
            posOrder.setTotal(roundedTotal);

            // 4. Save & Notify
            Order saved = orderService.saveOrderDirectly(posOrder);

            messagingTemplate.convertAndSend("/topic/orders", Map.of(
                    "order", saved,
                    "type", "NEW_ONLINE_ORDER",
                    "actingUser", "aggregator"));

            // --- UNIFIED ELITE NOTIFICATION DISPATCH ---
            try {
                System.out.println("[FORENSIC] Webhook DISPATCH for RID: " + restaurantId);
                List<String> tokens = authService.findTokensByRestaurantIdSmart(restaurantId);

                if (!tokens.isEmpty()) {
                    System.out.println("[FORENSIC] Webhook DISPATCH to " + tokens.size() + " tokens: " + tokens);
                    String title = "New " + platform + " Order!";
                    String body = "Order #" + externalId + " for ₹" + saved.getTotal();
                    Map<String, String> data = Map.of(
                            "type", "NEW_ONLINE_ORDER",
                            "orderId", saved.getId(),
                            "restaurantId", restaurantId,
                            "diagnostic_ts", String.valueOf(System.currentTimeMillis()));
                    fcmService.dispatchHighPrioritySiren(tokens, title, body, data);
                    System.out.println("[FORENSIC] Webhook Push sent to " + tokens.size() + " devices.");

                    // --- TRIGGER THE ATOMIC HAMMER ---
                    sirenSchedulerService.startSiren(saved);
                } else {
                    System.out.println("[FORENSIC] Webhook WARNING: No tokens found for RID: " + restaurantId);
                }
            } catch (Exception fcmEx) {
                System.err.println("[FORENSIC] Webhook CRITICAL PUSH FAILURE: " + fcmEx.getMessage());
            }

            return ResponseEntity.ok(Map.of("status", "success", "pos_order_id", saved.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private Product findProductByMapping(String restaurantId, String platform, String externalId) {
        List<Product> allProducts = productService.getAllProductsNoCache(restaurantId);
        // 1. Try explicit mapping first
        for (Product p : allProducts) {
            if (p.getExternalMappings() == null)
                continue;
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
            } catch (Exception e) {
            }
        }
        
        // 2. Fallback to naming convention: [prefix]_[productname]
        String pUpper = (platform != null ? platform.trim().toUpperCase() : "");
        String prefix;
        if ("GHARTAK".equals(pUpper)) {
            prefix = "gt";
        } else if ("SWIGGY".equals(pUpper)) {
            prefix = "sw";
        } else if ("ZOMATO".equals(pUpper)) {
            prefix = "zo";
        } else {
            prefix = platform.substring(0, Math.min(2, platform.length())).toLowerCase();
        }
        for (Product p : allProducts) {
            String fallbackId = prefix + "_" + p.getName().replaceAll("\\s+", "").toLowerCase();
            if (externalId.equalsIgnoreCase(fallbackId)) {
                return p;
            }
        }
        return null;
    }

}
