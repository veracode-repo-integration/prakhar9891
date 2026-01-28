package com.pos.server.service;

import com.pos.server.model.Product;
import com.pos.server.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod-" + UUID.randomUUID().toString());
        }
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public Optional<Product> getProduct(String id) {
        return productRepository.findById(id);
    }

    public Product updateProduct(String id, Product updates) {
        return productRepository.findById(id)
                .map(product -> {
                    if (updates.getName() != null)
                        product.setName(updates.getName());
                    if (updates.getPrice() > 0)
                        product.setPrice(updates.getPrice());
                    if (updates.getCategory() != null)
                        product.setCategory(updates.getCategory());
                    if (updates.getStock() >= 0)
                        product.setStock(updates.getStock());
                    if (updates.getImage() != null)
                        product.setImage(updates.getImage());
                    if (updates.getBarcode() != null)
                        product.setBarcode(updates.getBarcode());
                    if (updates.getComposition() != null)
                        product.setComposition(updates.getComposition());
                    return productRepository.save(product);
                })
                .orElse(null);
    }
}
