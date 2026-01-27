package com.pos.server.service;

import com.pos.server.model.Order;
import com.pos.server.model.OrderItem;
import com.pos.server.model.Product;
import com.pos.server.repository.OrderRepository;
import com.pos.server.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order createOrder(Order order) {
        // Reduce stock
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Optional<Product> pOpt = productRepository.findById(item.getId());
                if (pOpt.isPresent()) {
                    Product p = pOpt.get();
                    int newStock = Math.max(0, p.getStock() - item.getQty());
                    p.setStock(newStock);
                    productRepository.save(p);
                }
            }
        }
        // Save order
        return orderRepository.save(order);
    }
}
