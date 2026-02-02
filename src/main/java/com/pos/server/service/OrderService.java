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

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Order> getAllOrders(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return orderRepository.findAllByRestaurantId(restaurantId);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Order processCheckout(CheckoutRequest request) throws Exception {
        log.info("Processing normalized checkout for table: {}, type: {}, restaurant: {}", request.getTableId(),
                request.getOrderType(), request.getRestaurantId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setRestaurantId(request.getRestaurantId());

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

            // 1.1 Scoped Stock Reduction
            Optional<Product> pOpt = productRepository.findByIdAndRestaurantId(reqItem.getId(),
                    request.getRestaurantId());
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

            // Handle Customer Linkage (Create or Update)
            if (payInfo.containsKey("customer")) {
                Map<String, Object> custMap = (Map<String, Object>) payInfo.get("customer");
                String mobile = (String) custMap.get("mobile");
                String name = (String) custMap.get("name");

                if (mobile != null && !mobile.isEmpty()) {
                    List<Customer> customers = customerRepository.findByMobile(mobile);
                    // Filter by restaurantId if multiple customers found (unlikely but safe)
                    Customer customer = customers.stream()
                            .filter(c -> request.getRestaurantId() != null
                                    && request.getRestaurantId().equals(c.getRestaurantId()))
                            .findFirst()
                            .orElse(null);

                    if (customer != null) {
                        // Optional: Update name if provided and different
                        if (name != null && !name.isEmpty() && !name.equals(customer.getName())) {
                            customer.setName(name);
                            customerRepository.save(customer);
                        }
                    } else {
                        // Create new customer record
                        customer = new Customer();
                        customer.setId(UUID.randomUUID().toString());
                        customer.setMobile(mobile);
                        customer.setName(name != null ? name : "Walk-in Customer");
                        customer.setJoinedAt(new java.util.Date().toString());
                        customer.setRestaurantId(request.getRestaurantId());
                        customer = customerRepository.save(customer);
                    }
                    order.setCustomer(customer);
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
            tx.setRestaurantId(request.getRestaurantId());
            creditTransactionRepository.save(tx);
            log.info("Credit transaction recorded for Pay Later order: {}", savedOrder.getId());
        }

        return savedOrder;
    }

    @Transactional
    public Order updateOrder(String id, Order updates, String restaurantId) {
        Optional<Order> orderOpt = (restaurantId == null || restaurantId.trim().isEmpty())
                ? orderRepository.findById(id)
                : orderRepository.findByIdAndRestaurantId(id, restaurantId);

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
