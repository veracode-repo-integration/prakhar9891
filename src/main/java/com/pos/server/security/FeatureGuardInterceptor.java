package com.pos.server.security;

import com.pos.server.model.User;
import com.pos.server.model.Restaurant;
import com.pos.server.repository.RestaurantRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

@Component
public class FeatureGuardInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();

        // 1. Skip open endpoints and OPTIONS requests
        if (request.getMethod().equalsIgnoreCase("OPTIONS") ||
                uri.startsWith("/api/auth/") ||
                uri.startsWith("/ws-pos") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/api/superadmin") ||
                uri.startsWith("/api/dine-in/")) {
            return true;
        }

        // 2. Identify required feature based on exact URL paths
        String requiredFeature = null;

        if (uri.startsWith("/api/expenses")) {
            requiredFeature = "EXPENSES";
        } else if (uri.startsWith("/api/customers")) {
            requiredFeature = "CUSTOMERS";
        } else if (uri.startsWith("/api/orders/kitchen")) {
            requiredFeature = "KITCHEN";
        } else if (uri.startsWith("/api/tables")) {
            requiredFeature = "TABLES";
        }

        // If no specific feature is mapped, allow the request
        if (requiredFeature == null) {
            return true;
        }

        // 3. Authenticate User & Validate Feature
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {

            // Super Admins bypass all restrictions
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                return true;
            }

            String username = auth.getName();
            User user = authService.findByUsername(username);

            if (user != null && user.getRestaurantId() != null) {
                Optional<Restaurant> restaurantOpt = restaurantRepository.findById(user.getRestaurantId());
                if (restaurantOpt.isPresent()) {
                    List<String> allowedFeatures = restaurantOpt.get().getAllowedFeatures();

                    // If allowedFeatures is empty or null, we assume they are grandfathered in /
                    // have full access
                    if (allowedFeatures == null || allowedFeatures.isEmpty()) {
                        return true;
                    }

                    if (!allowedFeatures.contains(requiredFeature)) {
                        response.setStatus(403);
                        response.getWriter()
                                .write("{\"ok\": false, \"message\": \"Your subscription does not include access to "
                                        + requiredFeature + "\"}");
                        response.setContentType("application/json");
                        return false;
                    }
                    return true;
                }
            }
        }

        return true;
    }
}
