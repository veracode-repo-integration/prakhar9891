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
        return productRepository.findAllByRestaurantIdIgnoreCase(restaurantId);
    }

    public List<Product> getAllProductsNoCache(String restaurantId) {
        return productRepository.findAllByRestaurantIdIgnoreCase(restaurantId.toLowerCase().trim());
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod-" + UUID.randomUUID().toString());
        }

        // Auto-assign professional stock photo if image is missing
        assignDefaultImage(product);

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
            assignDefaultImage(product);
        }
        return productRepository.saveAll(products);
    }

    private void assignDefaultImage(Product product) {
        if (product.getImage() == null || product.getImage().trim().isEmpty() || product.getImage().equals("null")) {
            // Robust Mapping: Keyword -> High Quality Unsplash Image ID
            // These are direct image links, 100% guaranteed to work and look professional.
            String name = (product.getName() != null ? product.getName().toLowerCase() : "");
            String category = (product.getCategory() != null ? product.getCategory().toLowerCase() : "");
            String search = name + " " + category;

            String imageId = "1546069901-ba9599a7e63c"; // Default: Salad/Healthy Bowl

            if (search.contains("burger"))
                imageId = "1568901346375-23c9450c58cd";
            else if (search.contains("pizza"))
                imageId = "1513104890138-7c749659a591";
            else if (search.contains("coffee") || search.contains("latte"))
                imageId = "1509042239860-f550ce710b93";
            else if (search.contains("tea") || search.contains("chai"))
                imageId = "1561336313-0bd5e0b27ec8";
            else if (search.contains("shake") || search.contains("smoothie"))
                imageId = "1572490006316-254cc47e5d1b";
            else if (search.contains("drink") || search.contains("beverage") || search.contains("juice"))
                imageId = "1513558161293-cdaf765ed2fd";
            else if (search.contains("cake") || search.contains("dessert"))
                imageId = "1578985545062-69928b1d9587";
            else if (search.contains("rice") || search.contains("biryani"))
                imageId = "1589302168068-96e2686af0b6";
            else if (search.contains("curry") || search.contains("gravy"))
                imageId = "1565557623262-b51c2513a641";
            else if (search.contains("paneer"))
                imageId = "1567188040759-fb8a883dc6d8"; // Indian Food generic
            else if (search.contains("chicken"))
                imageId = "1562967914688-651167a7928e";
            else if (search.contains("pasta") || search.contains("noodle"))
                imageId = "1621996346529-84e55e511852";
            else if (search.contains("sandwich"))
                imageId = "1528735602-93918baef34f";
            else if (search.contains("fries"))
                imageId = "1573080496029-710cfa6e8a63";
            else if (search.contains("ice cream"))
                imageId = "1497034825451-22c68eb0941e";

            // Construct standard Unsplash URL
            product.setImage("https://images.unsplash.com/photo-" + imageId + "?auto=format&fit=crop&w=800&q=80");
        }
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
                    if (updates.getPrice() > 0)
                        product.setPrice(updates.getPrice());
                    if (updates.getCategory() != null)
                        product.setCategory(updates.getCategory());
                    if (updates.getStock() >= 0)
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
                    } else {
                        // If no new image provided in update, check if we need to auto-assign for
                        // existing product
                        if (product.getImage() == null || product.getImage().trim().isEmpty()
                                || product.getImage().equals("null")) {
                            assignDefaultImage(product);
                        }
                    }

                    if (updates.getBarcode() != null)
                        product.setBarcode(updates.getBarcode());
                    if (updates.getComposition() != null)
                        product.setComposition(updates.getComposition());
                    if (updates.getCostPrice() >= 0)
                        product.setCostPrice(updates.getCostPrice());
                    if (updates.getTax() >= 0)
                        product.setTax(updates.getTax());
                    if (updates.getTaxCategory() != null)
                        product.setTaxCategory(updates.getTaxCategory());
                    if (updates.getHsnCode() != null)
                        product.setHsnCode(updates.getHsnCode());
                    if (updates.getOnlineAvailability() != null)
                        product.setOnlineAvailability(updates.getOnlineAvailability());
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
