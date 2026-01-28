package com.pos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.server.dto.CheckoutRequest;
import com.pos.server.model.Order;
import com.pos.server.model.OrderItem;
import com.pos.server.model.Product;
import com.pos.server.repository.OrderRepository;
import com.pos.server.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order processCheckout(CheckoutRequest request) throws Exception {
        log.info("Processing checkout for table: {}, type: {}", request.getTableId(), request.getOrderType());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }

        Order order = new Order();
        order.setId("ORD-" + System.currentTimeMillis());
        order.setItems(request.getItems());
        order.setCreatedAt(System.currentTimeMillis());
        order.setDate(java.time.Instant.now().toString());

        // Payment Info & Customer
        if (request.getPaymentInfo() != null) {
            order.setMethod((String) request.getPaymentInfo().get("method"));
            order.setNote((String) request.getPaymentInfo().get("note"));
            order.setPaymentInfoJson(mapper.writeValueAsString(request.getPaymentInfo()));

            if (request.getPaymentInfo().containsKey("customer")) {
                order.setCustomerJson(mapper.writeValueAsString(request.getPaymentInfo().get("customer")));
            }
        }

        // User Mapping
        if (request.getUser() != null) {
            Object userObj = request.getUser();
            if (userObj instanceof String) {
                order.setUserJson((String) userObj);
                order.setUser((String) userObj);
            } else {
                order.setUserJson(mapper.writeValueAsString(userObj));
                if (userObj instanceof Map) {
                    order.setUser((String) ((Map) userObj).getOrDefault("username", "unknown"));
                }
            }
        }

        // Financials
        double subtotal = request.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQty()).sum();
        order.setSubtotal(subtotal);
        if (request.getTotal() > 0) {
            order.setTotal(request.getTotal());
            order.setTaxAmount(request.getTaxAmount());
            order.setTaxRate(request.getTaxRate());
        } else {
            order.setTotal(subtotal);
        }

        if (request.getDiscount() != null) {
            order.setDiscountJson(mapper.writeValueAsString(request.getDiscount()));
        }

        // Meta Data
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "dine-in");
        order.setTableId(request.getTableId());
        order.setPayLater(request.isPayLater());
        order.setPayLaterAmount(request.getPayLaterAmount());
        order.setPointsEarned(request.getPointsEarned());

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        } else {
            order.setStatus(request.isPayLater() ? "pending" : "completed");
        }

        // Stock Reduction logic
        for (OrderItem item : order.getItems()) {
            Optional<Product> pOpt = productRepository.findById(item.getId());
            if (pOpt.isPresent()) {
                Product p = pOpt.get();
                int newStock = Math.max(0, p.getStock() - item.getQty());
                p.setStock(newStock);
                productRepository.save(p);
                log.debug("Updated stock for product {}: {} -> {}", p.getId(), p.getStock() + item.getQty(), newStock);
            }
        }

        Order saved = orderRepository.save(order);
        log.info("Order saved successfully: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Order createOrder(Order order) {
        // Keeping this for backward compatibility if other services use it
        return orderRepository.save(order);
    }
}
