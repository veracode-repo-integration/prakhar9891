package com.pos.server.service;

import com.pos.server.model.Product;
import com.pos.server.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void testGetAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(new Product(), new Product()));
        List<Product> products = productService.getAllProducts();
        assertEquals(2, products.size());
    }

    @Test
    void testSaveProduct() {
        Product p = new Product();
        p.setName("Test Product");
        when(productRepository.save(any(Product.class))).thenReturn(p);

        Product saved = productService.saveProduct(p);
        assertNotNull(saved);
        assertEquals("Test Product", saved.getName());
    }

    @Test
    void testDeleteProduct() {
        doNothing().when(productRepository).deleteById("123");
        productService.deleteProduct("123");
        verify(productRepository, times(1)).deleteById("123");
    }
}
