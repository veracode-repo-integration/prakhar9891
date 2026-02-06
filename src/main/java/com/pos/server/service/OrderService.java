package com.pos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.server.dto.CheckoutRequest;
import com.pos.server.model.*;
import com.pos.server.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
// Force Deploy Fix
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private StockLogRepository stockLogRepository;

    @Autowired
    private CommonDataService commonDataService;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Order> getAllOrders(String restaurantId) {
        log.info("Fetching orders for restaurant: {}", restaurantId);
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return orderRepository.findAllByRestaurantId(restaurantId.toLowerCase().trim());
    }

    public org.springframework.data.domain.Page<Order> getOrdersPaged(String restaurantId, int page, int size) {
        log.info("Fetching orders V2 for restaurant: {}, page: {}, size: {}", restaurantId, page, size);
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return org.springframework.data.domain.Page.empty();
        }
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return orderRepository.findAllByRestaurantId(restaurantId.toLowerCase().trim(), p);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Order processCheckout(CheckoutRequest request) throws Exception {
        String rid = request.getRestaurantId() != null ? request.getRestaurantId().toLowerCase().trim() : "default";

        log.info("Processing normalized checkout for table: {}, type: {}, restaurant: {}", request.getTableId(),
                request.getOrderType(), rid);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setRestaurantId(rid);
        order.setInvoiceNumber(commonDataService.getNextInvoiceNumber(rid));

        order.setCreatedAt(System.currentTimeMillis());
        order.setDate(java.time.Instant.now().toString());
        order.setKitchenStatus(request.getKitchenStatus() != null ? request.getKitchenStatus() : "pending");
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "dine-in");
        order.setTableId(request.getTableId());

        String username = null;
        if (request.getUser() != null) {
            Object userObj = request.getUser();
            if (userObj instanceof String) {
                username = (String) userObj;
            } else if (userObj instanceof Map) {
                username = (String) ((Map) userObj).get("username");
            }
        }

        // Fetch settings to check if stock tracking is disabled
        Settings settings = commonDataService.getSettings(rid);
        boolean disableStockTracking = Boolean.TRUE.equals(settings.getDisableStockDecrement());
        log.info("[OrderService] Checkout for restaurant: {}. SettingsID: {}. Stock Tracking Disabled: {}",
                rid, settings.getId(), disableStockTracking);

        // 1. Map Items (Normalize Modifiers)
        List<OrderItem> items = new ArrayList<>();
        for (OrderItem reqItem : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setItemId(UUID.randomUUID().toString());
            item.setProductId(reqItem.getProductId()); // Product ID (Matches frontend 'id')
            item.setName(reqItem.getName());
            item.setPrice(reqItem.getPrice());
            item.setQty(reqItem.getQty());
            item.setModifiers(reqItem.getModifiers());
            item.setOrder(order);
            items.add(item);

            // 1.1 Scoped Stock Reduction & Logging (Skip if disabled)
            if (!disableStockTracking) {
                Optional<Product> pOpt = productRepository.findByIdAndRestaurantId(reqItem.getProductId(),
                        rid);
                if (pOpt.isPresent()) {
                    Product p = pOpt.get();
                    int oldStock = p.getStock();
                    p.setStock(Math.max(0, oldStock - reqItem.getQty()));
                    productRepository.save(p);

                    // Create Stock Log
                    StockLog logEntry = new StockLog();
                    logEntry.setId(UUID.randomUUID().toString());
                    logEntry.setProductId(p.getId());
                    logEntry.setQtyChange(-reqItem.getQty());
                    logEntry.setType("sale");
                    logEntry.setReason("Sale: Order #" + order.getId());
                    logEntry.setUser(username != null ? username : "system");
                    logEntry.setCreatedAt(java.time.Instant.now().toString());
                    logEntry.setRestaurantId(rid);
                    stockLogRepository.save(logEntry);
                }
            }
        }
        order.setItems(items);

        // 2. Map Payment & Customer (Normalize)
        if (request.getPaymentInfo() != null) {
            Map<String, Object> payInfo = request.getPaymentInfo();
            log.info("Processing payments for order: {} with method: {}", order.getId(), payInfo.get("method"));
            order.setMethod((String) payInfo.get("method"));
            order.setNote((String) payInfo.get("note"));

            // Handle Payments List
            List<PaymentDetail> payments = new ArrayList<>();
            Object splitPayments = payInfo.get("payments");
            if (splitPayments instanceof List && !((List<?>) splitPayments).isEmpty()) {
                log.info("Detected split payments for order: {}", order.getId());
                for (Map<String, Object> p : (List<Map<String, Object>>) splitPayments) {
                    PaymentDetail pd = new PaymentDetail();
                    pd.setMethod((String) p.get("method"));
                    pd.setAmount(Double.parseDouble(p.get("amount").toString()));
                    pd.setReference((String) p.get("reference"));
                    pd.setOrder(order);
                    payments.add(pd);
                }
            } else {
                log.info("Detected single payment for order: {}", order.getId());
                PaymentDetail pd = new PaymentDetail();
                pd.setMethod((String) payInfo.get("method"));
                pd.setAmount(request.getTotal());
                pd.setOrder(order);
                payments.add(pd);
            }
            order.setPayments(payments);

            // Handle Customer Linkage (Create or Update)
            if (payInfo.containsKey("customer") && payInfo.get("customer") != null) {
                Map<String, Object> custMap = (Map<String, Object>) payInfo.get("customer");
                String mobile = (String) custMap.get("mobile");
                String name = (String) custMap.get("name");

                if (mobile != null && !mobile.trim().isEmpty()) {
                    String normMobile = mobile.trim();
                    log.info("Linking customer with mobile: {} to order: {}", normMobile, order.getId());
                    Customer customer = customerRepository.findByMobileAndRestaurantId(normMobile, rid).orElse(null);

                    if (customer != null) {
                        log.info("Found existing customer ID: {} for mobile: {}", customer.getId(), normMobile);
                        if (name != null && !name.trim().isEmpty()) {
                            customer.setName(name.trim());
                        }
                        customer.setRestaurantId(rid);
                        customer = customerRepository.saveAndFlush(customer);
                    } else {
                        log.info("Creating new customer profile for mobile: {}", normMobile);
                        customer = new Customer();
                        customer.setId(UUID.randomUUID().toString());
                        customer.setMobile(normMobile);
                        customer.setName((name != null && !name.trim().isEmpty()) ? name.trim() : "Walk-in Customer");
                        customer.setJoinedAt(java.time.Instant.now().toString());
                        customer.setRestaurantId(rid);
                        customer = customerRepository.saveAndFlush(customer);
                    }
                    order.setCustomer(customer);
                }
            }
        }

        // 3. User Linkage
        if (username != null) {
            order.setUser(username);
            log.info("Linking creator user: {} to order: {}", username, order.getId());
            Optional<User> uOpt = userRepository.findByUsername(username);
            if (uOpt.isPresent()) {
                order.setCreator(uOpt.get());
            } else {
                log.warn("Creator user '{}' not found in database for order: {}", username, order.getId());
            }
        }

        // 4. Financials
        order.setSubtotal(request.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQty()).sum());
        order.setTaxRate(request.getTaxRate());
        order.setTaxAmount(request.getTaxAmount());
        order.setTotal(request.getTotal());

        if (request.getDiscount() != null) {
            order.setDiscountType((String) request.getDiscount().get("type"));
            order.setDiscountValue(Double.parseDouble(request.getDiscount().get("value").toString()));
        }

        order.setPayLater(request.isPayLater());
        order.setPayLaterAmount(request.getPayLaterAmount());
        order.setPointsEarned(request.getPointsEarned());
        order.setStatus(
                request.getStatus() != null ? request.getStatus() : (request.isPayLater() ? "pending" : "completed"));

        Order savedOrder = orderRepository.saveAndFlush(order);

        // --- VACATE TABLE IF DINE-IN ---
        // --- VACATE TABLE IF DINE-IN ---
        if (request.getTableId() != null && !request.getTableId().isEmpty()) {
            List<RestaurantTable> tables = tableRepository.findAllByIdAndRestaurantId(request.getTableId(), rid);

            if (!tables.isEmpty()) {
                RestaurantTable table = tables.get(0); // Take the first one if duplicates exist
                if (tables.size() > 1) {
                    log.warn("Found {} duplicate tables for ID {} in restaurant {}. Using first result.", tables.size(),
                            request.getTableId(), rid);
                }

                table.setStatus("available");
                table.setCurrentOrder(null);
                table.setGuestCount(0);
                table.setSessionStart(null);
                table.setKitchenStatus(null);
                tableRepository.save(table);
                log.info("Table {} vacated successfully after checkout", request.getTableId());
            } else {
                log.warn("Table {} not found for vacating after checkout in restaurant {}", request.getTableId(), rid);
            }
        }

        // --- ADD TO CREDIT LEDGER IF PAY LATER ---
        if (savedOrder.isPayLater() && savedOrder.getCustomer() != null) {
            CreditTransaction tx = new CreditTransaction();
            tx.setId("ORD-" + savedOrder.getId());
            tx.setMobile(savedOrder.getCustomer().getMobile());
            tx.setName(savedOrder.getCustomer().getName());
            tx.setType("order");
            tx.setAmount(savedOrder.getTotal());
            tx.setDate(System.currentTimeMillis());
            tx.setOrderId(savedOrder.getId());
            tx.setNote("Pay Later Order #" + savedOrder.getId());
            tx.setRestaurantId(rid);
            creditTransactionRepository.save(tx);
            log.info("Credit transaction recorded for Pay Later order: {}", savedOrder.getId());
        }

        return savedOrder;
    }

    @Transactional
    public Order updateOrder(String id, Order updates, String restaurantId) {
        // Search by ID first to be resilient to restaurantId drifting
        Optional<Order> orderOpt = orderRepository.findById(id);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Security check: if restaurantId is provided, it must match
            if (restaurantId != null && order.getRestaurantId() != null
                    && !order.getRestaurantId().equalsIgnoreCase(restaurantId)) {
                log.warn("Access denied for order {} update. Req RID: {}, Order RID: {}", id, restaurantId,
                        order.getRestaurantId());
                return null;
            }

            if (updates.getStatus() != null)
                order.setStatus(updates.getStatus());
            if (updates.getKitchenStatus() != null)
                order.setKitchenStatus(updates.getKitchenStatus());

            // Refund logic
            if (updates.getPartialRefundAmount() > 0)
                order.setPartialRefundAmount(updates.getPartialRefundAmount());
            if (updates.getRefundedTaxAmount() > 0)
                order.setRefundedTaxAmount(updates.getRefundedTaxAmount());
            if (updates.getRefundedAt() != null)
                order.setRefundedAt(updates.getRefundedAt());
            if (updates.getRefundMethod() != null)
                order.setRefundMethod(updates.getRefundMethod());
            if (updates.getRefundedItemsJson() != null) {
                order.setRefundedItemsJson(updates.getRefundedItemsJson());
                // Handle Stock Restoration if Reusable
                if (Boolean.TRUE.equals(updates.getReusable())) {
                    order.setReusable(true);
                    try {
                        List<Map<String, Object>> items = mapper.readValue(updates.getRefundedItemsJson(), List.class);
                        for (Map<String, Object> item : items) {
                            String productId = (String) item.get("id");
                            if (productId == null)
                                productId = (String) item.get("productId");
                            if (productId != null) {
                                int qty = (item.get("qty") != null) ? ((Number) item.get("qty")).intValue() : 0;
                                Optional<Product> pOpt = productRepository.findByIdAndRestaurantId(productId,
                                        order.getRestaurantId());
                                if (pOpt.isPresent()) {
                                    Product p = pOpt.get();
                                    p.setStock(p.getStock() + qty);
                                    productRepository.save(p);

                                    StockLog logEntry = new StockLog();
                                    logEntry.setId(UUID.randomUUID().toString());
                                    logEntry.setProductId(p.getId());
                                    logEntry.setQtyChange(qty);
                                    logEntry.setType("restock");
                                    logEntry.setReason("Refund Restock: Order #" + order.getId());
                                    logEntry.setUser(order.getUser() != null ? order.getUser() : "system");
                                    logEntry.setCreatedAt(java.time.Instant.now().toString());
                                    logEntry.setRestaurantId(order.getRestaurantId());
                                    stockLogRepository.save(logEntry);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to process refund stock restoration for order: {}", id, e);
                    }
                }
            }

            return orderRepository.save(order);
        }
        return null;
    }
}
