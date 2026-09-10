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

    @Autowired
    private GCSService gcsService;

    public List<Product> getAllProducts(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Product> products = productRepository.findAllByRestaurantIdIgnoreCase(restaurantId);
        products.forEach(p -> {
            if (p.getImage() != null && p.getImage().contains("unsplash.com")) {
                p.setImage("");
            }
        });
        return products;
    }

    public List<Product> getAllProductsNoCache(String restaurantId) {
        List<Product> products = productRepository.findAllByRestaurantIdIgnoreCase(restaurantId.toLowerCase().trim());
        products.forEach(p -> {
            if (p.getImage() != null && p.getImage().contains("unsplash.com")) {
                p.setImage("");
            }
        });
        return products;
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod-" + UUID.randomUUID().toString());
        }

        // Handle Image Upload if base64
        try {
            if (product.getImage() != null && product.getImage().startsWith("data:image")) {
                String imageUrl = gcsService.uploadBase64Image(product.getImage(), "products");
                product.setImage(imageUrl);
            }
        } catch (Exception e) {
            System.err.println("GCS Upload failed for product: " + e.getMessage());
        }

        return productRepository.save(product);
    }

    public List<Product> saveAllProducts(List<Product> products) {
        for (Product product : products) {
            if (product.getId() == null || product.getId().isEmpty()) {
                product.setId("prod-" + UUID.randomUUID().toString());
            }
        }
        return productRepository.saveAll(products);
    }

    public int clearAllProductImages(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return 0;
        }
        List<Product> products = productRepository.findAllByRestaurantIdIgnoreCase(restaurantId);
        int count = 0;
        for (Product p : products) {
            if (p.getImage() != null && !p.getImage().trim().isEmpty()) {
                p.setImage("");
                count++;
            }
        }
        if (count > 0) {
            productRepository.saveAll(products);
        }
        return count;
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public boolean deleteProductByIdAndRestaurantId(String id, String restaurantId) {
        return productRepository.findByIdAndRestaurantId(id, restaurantId).map(product -> {
            productRepository.delete(product);
            return true;
        }).orElse(false);
    }

    public Optional<Product> getProduct(String id) {
        return productRepository.findById(id);
    }

    public Product updateProduct(String id, Product updates) {
        return productRepository.findById(id)
                .map(product -> {
                    if (updates.getName() != null)
                        product.setName(updates.getName());
                    if (updates.getPrice() != null)
                        product.setPrice(updates.getPrice());
                    if (updates.getCategory() != null)
                        product.setCategory(updates.getCategory());
                    if (updates.getStock() != null)
                        product.setStock(updates.getStock());

                    if (updates.getImage() != null) {
                        try {
                            if (updates.getImage().startsWith("data:image")) {
                                String imageUrl = gcsService.uploadBase64Image(updates.getImage(), "products");
                                product.setImage(imageUrl);
                            } else {
                                product.setImage(updates.getImage());
                            }
                        } catch (Exception e) {
                            System.err.println("GCS Upload failed during update: " + e.getMessage());
                            product.setImage(updates.getImage());
                        }
                    }

                    if (updates.getBarcode() != null)
                        product.setBarcode(updates.getBarcode());
                    if (updates.getComposition() != null)
                        product.setComposition(updates.getComposition());
                    if (updates.getCostPrice() != null)
                        product.setCostPrice(updates.getCostPrice());
                    if (updates.getTax() != null)
                        product.setTax(updates.getTax());
                    if (updates.getTaxCategory() != null)
                        product.setTaxCategory(updates.getTaxCategory());
                    if (updates.getHsnCode() != null)
                        product.setHsnCode(updates.getHsnCode());
                    if (updates.getOnlineAvailability() != null)
                        product.setOnlineAvailability(updates.getOnlineAvailability());
                    if (updates.getExternalMappings() != null)
                        product.setExternalMappings(updates.getExternalMappings());

                    return productRepository.save(product);
                })
                .orElse(null);
    }

    public Product updateOnlineAvailability(String id, String status) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setOnlineAvailability(status);
                    return productRepository.save(product);
                })
                .orElse(null);
    }
}
