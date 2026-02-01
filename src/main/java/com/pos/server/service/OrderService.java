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

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Order processCheckout(CheckoutRequest request) throws Exception {
        log.info("Processing normalized checkout for table: {}, type: {}", request.getTableId(),
                request.getOrderType());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }

        Order order = new Order();
        // GOAL 1: DUPLICATE ID FIX - Use UUID or check for existing ID from frontend
        // If frontend sends an ID, use it (it should be a UUID now), otherwise generate
        // Actually, I'll prefer UUID for production.
        order.setId(UUID.randomUUID().toString());

        order.setCreatedAt(System.currentTimeMillis());
        order.setDate(java.time.Instant.now().toString());
        order.setKitchenStatus(request.getKitchenStatus() != null ? request.getKitchenStatus() : "pending");
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "dine-in");
        order.setTableId(request.getTableId());

        // 1. Map Items (Normalize Modifiers)
        List<OrderItem> items = new ArrayList<>();
        for (OrderItem reqItem : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setId(reqItem.getId());
            item.setName(reqItem.getName());
            item.setPrice(reqItem.getPrice());
            item.setQty(reqItem.getQty());
            item.setModifiers(reqItem.getModifiers());
            item.setOrder(order);
            items.add(item);

            // Stock Reduction
            Optional<Product> pOpt = productRepository.findById(reqItem.getId());
            if (pOpt.isPresent()) {
                Product p = pOpt.get();
                p.setStock(Math.max(0, p.getStock() - reqItem.getQty()));
                productRepository.save(p);
            }
        }
        order.setItems(items);

        // 2. Map Payment & Customer (Normalize)
        if (request.getPaymentInfo() != null) {
            Map<String, Object> payInfo = request.getPaymentInfo();
            order.setMethod((String) payInfo.get("method"));
            order.setNote((String) payInfo.get("note"));

            // Handle Payments List
            List<PaymentDetail> payments = new ArrayList<>();
            Object splitPayments = payInfo.get("payments");
            if (splitPayments instanceof List) {
                for (Map<String, Object> p : (List<Map<String, Object>>) splitPayments) {
                    PaymentDetail pd = new PaymentDetail();
                    pd.setMethod((String) p.get("method"));
                    pd.setAmount(Double.parseDouble(p.get("amount").toString()));
                    pd.setReference((String) p.get("reference"));
                    pd.setOrder(order);
                    payments.add(pd);
                }
            } else {
                // Single payment
                PaymentDetail pd = new PaymentDetail();
                pd.setMethod((String) payInfo.get("method"));
                pd.setAmount(request.getTotal());
                pd.setOrder(order);
                payments.add(pd);
            }
            order.setPayments(payments);

            // Handle Customer Linkage
            if (payInfo.containsKey("customer")) {
                Map<String, Object> custMap = (Map<String, Object>) payInfo.get("customer");
                String mobile = (String) custMap.get("mobile");
                if (mobile != null) {
                    List<Customer> customers = customerRepository.findByMobile(mobile);
                    if (!customers.isEmpty()) {
                        order.setCustomer(customers.get(0));
                    }
                }
            }
        }

        // 3. User Linkage
        if (request.getUser() != null) {
            Object userObj = request.getUser();
            String username = null;
            if (userObj instanceof String) {
                username = (String) userObj;
            } else if (userObj instanceof Map) {
                username = (String) ((Map) userObj).get("username");
            }

            if (username != null) {
                order.setUser(username);
                Optional<User> uOpt = userRepository.findByUsername(username);
                uOpt.ifPresent(order::setCreator);
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

        Order savedOrder = orderRepository.save(order);

        // --- VACATE TABLE IF DINE-IN ---
        if (request.getTableId() != null && !request.getTableId().isEmpty()) {
            tableRepository.findById(request.getTableId()).ifPresent(table -> {
                table.setStatus("available");
                table.setCurrentOrder(null);
                table.setGuestCount(0);
                table.setSessionStart(null);
                table.setKitchenStatus(null);
                tableRepository.save(table);
                log.info("Table {} vacated successfully after checkout", request.getTableId());
            });
        }

        return savedOrder;
    }

    @Transactional
    public Order updateOrder(String id, Order updates) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (updates.getStatus() != null)
                order.setStatus(updates.getStatus());
            if (updates.getKitchenStatus() != null)
                order.setKitchenStatus(updates.getKitchenStatus());

            // Refund Login
            if (updates.getPartialRefundAmount() > 0)
                order.setPartialRefundAmount(updates.getPartialRefundAmount());
            if (updates.getRefundedTaxAmount() > 0)
                order.setRefundedTaxAmount(updates.getRefundedTaxAmount());
            if (updates.getRefundedAt() != null)
                order.setRefundedAt(updates.getRefundedAt());
            if (updates.getRefundMethod() != null)
                order.setRefundMethod(updates.getRefundMethod());
            if (updates.getRefundedItemsJson() != null)
                order.setRefundedItemsJson(updates.getRefundedItemsJson());

            return orderRepository.save(order);
        }
        return null;
    }
}
