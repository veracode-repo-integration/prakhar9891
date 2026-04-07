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
    public ResponseEntity<List<Product>> getProducts(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(productService.getAllProducts(rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestBody Product product,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = authService.validateRestaurantAccess(restaurantId);
        product.setRestaurantId(rid);
        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Product>> bulkSaveProducts(@RequestBody List<Product> products,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = authService.validateRestaurantAccess(restaurantId);
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
        String rid = authService.validateRestaurantAccess(restaurantId);
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
        String rid = authService.validateRestaurantAccess(restaurantId);
        updates.setRestaurantId(rid);
        Product updated = productService.updateProduct(id, updates);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("ok", true, "product", updated));
        }
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Product not found"));
    }

    @PutMapping("/{id}/online-availability")
    public ResponseEntity<?> updateOnlineAvailability(@PathVariable String id, @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = authService.validateRestaurantAccess(restaurantId);
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "message", "Status is required"));
        }
        Product updated = productService.updateOnlineAvailability(id, status);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("ok", true, "product", updated));
        }
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Product not found"));
    }

}
