package com.pos.server.service;

import com.pos.server.model.Order;
import com.pos.server.model.OrderItem;
import com.pos.server.model.Product;
import com.pos.server.repository.OrderRepository;
import com.pos.server.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testCreateOrder() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setId("prod-1");
        item.setQty(2);
        order.setItems(List.of(item));

        Product product = new Product();
        product.setId("prod-1");
        product.setStock(10);

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order created = orderService.createOrder(order);
        assertNotNull(created);

        // precise verification of stock reduction logic
        verify(productRepository, times(1)).save(argThat(p -> p.getStock() == 8));
    }
}
