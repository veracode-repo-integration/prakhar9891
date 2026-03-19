package com.pos.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private com.pos.server.repository.RestaurantRepository restaurantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (request.getServletPath().startsWith("/api/superadmin")) {
            System.out.println("[Security] SuperAdmin Request: " + request.getServletPath() + " Header: "
                    + (authHeader != null ? "Present" : "MISSING"));
        }
        String token = null;
        String username = null;
        String role = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                username = jwtUtils.getUsernameFromToken(token);
                role = jwtUtils.getRoleFromToken(token);
                String restaurantId = jwtUtils.getRestaurantIdFromToken(token);

                // Subscription Check: Block all non-superadmin requests if restaurant is
                // suspended
                if (restaurantId != null && !"SUPER_ADMIN".equals(role)) {
                    boolean isSuspended = restaurantRepository.findById(restaurantId)
                            .map(r -> "SUSPENDED".equalsIgnoreCase(r.getStatus()))
                            .orElse(false);
                    if (isSuspended) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"ok\":false,\"message\":\"Your account is suspended. Please connect 9891260801\"}");
                        return;
                    }
                }
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
                if (role != null && !role.isEmpty()) {
                    authorities = java.util.Collections.singletonList(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_" + role.toUpperCase()));
                } else {
                    authorities = userDetails.getAuthorities();
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                System.err.println("Error setting authentication: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
