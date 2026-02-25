package com.pos.server.controller;

import com.pos.server.model.Product;
import com.pos.server.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/pos-products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private com.pos.server.service.AuthService authService;

    @GetMapping
    public List<Product> getProducts(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = validateRestaurantAccess(restaurantId);
        return productService.getAllProducts(rid);
    }

    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestBody Product product,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = validateRestaurantAccess(restaurantId);
        product.setRestaurantId(rid);
        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Product>> bulkSaveProducts(@RequestBody List<Product> products,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = validateRestaurantAccess(restaurantId);
        for (Product product : products) {
            product.setRestaurantId(rid);
        }
        List<Product> saved = productService.saveAllProducts(products);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        // Security: Validate ownership before deleting
        String rid = validateRestaurantAccess(restaurantId);
        if (rid != null) {
            // Only delete if the product belongs to this restaurant
            boolean deleted = productService.deleteProductByIdAndRestaurantId(id, rid);
            if (!deleted) {
                return ResponseEntity.status(404)
                        .body(Map.of("ok", false, "message", "Product not found or access denied"));
            }
        } else {
            productService.deleteProduct(id);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product updates,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = validateRestaurantAccess(restaurantId);
        updates.setRestaurantId(rid);
        Product updated = productService.updateProduct(id, updates);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("ok", true, "product", updated));
        }
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Product not found"));
    }

    private String validateRestaurantAccess(String headerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new RuntimeException("Unauthorized");

        String username = auth.getName();
        com.pos.server.model.User user = authService.findByUsername(username);

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            if (headerId != null && !headerId.trim().isEmpty())
                return headerId.toLowerCase().trim();
            return user.getRestaurantId() != null ? user.getRestaurantId().toLowerCase().trim() : null;
        }

        if (user.getRestaurantId() == null || user.getRestaurantId().isEmpty()) {
            throw new RuntimeException("User not assigned to any restaurant");
        }
        return user.getRestaurantId().toLowerCase().trim();
    }
}
