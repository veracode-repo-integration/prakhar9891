package com.pos.server.service;

import com.pos.server.model.User;
import com.pos.server.model.Restaurant;
import com.pos.server.repository.UserRepository;
import com.pos.server.repository.RestaurantRepository;
import com.pos.server.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import com.pos.server.security.JwtUtils;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.pos.server.security.FirebaseService firebaseService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private JwtUtils jwtUtils;


    @org.springframework.beans.factory.annotation.Value("${superadmin.phones:9891260801}")
    private String superAdminPhones;

    private User assignSuperAdminIfApplicable(User user, String cleanPhone) {
        if (superAdminPhones != null && !superAdminPhones.isEmpty()) {
            boolean isSuperAdmin = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .filter(p -> p.length() >= 10)
                    .anyMatch(p -> p.equals(cleanPhone) || (cleanPhone.length() >= 10 && cleanPhone.endsWith(p)));
            if (isSuperAdmin) {
                user.setRole("SUPER_ADMIN");
                // DO NOT save to repository. This role is transient for the session.
                return user;
            }
        }
        return user;
    }

    public User register(com.pos.server.dto.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setContact(request.getContact());
        user.setRole(request.getRole() != null ? request.getRole() : "STAFF");
        user.setRestaurantId(request.getRestaurantId() != null ? request.getRestaurantId().toLowerCase().trim() : null);

        User saved = userRepository.save(user);
        return saved;
    }

    public User login(String identifier, String providedPassword) {
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findFirstByContact(identifier);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(providedPassword, user.getPasswordHash())) {
                // Ensure Super Admin status is reassigned even on password login
                user = assignSuperAdminIfApplicable(user, user.getContact() != null ? user.getContact().replaceAll("\\D", "") : "");
                checkSubscription(user);
                return user;
            }
        }
        throw new RuntimeException("Invalid credentials");
    }

    public void deleteUser(String username) {
        User user = findByUsername(username);
        // Robust Deletion: Instead of physical delete which breaks foreign keys (orders, etc.)
        // We anonymize the user to "free up" the username/contact and scrub all PII details.
        String timestamp = String.valueOf(System.currentTimeMillis());
        user.setUsername("deleted_" + user.getUsername() + "_" + timestamp);
        user.setContact("deleted_" + timestamp);
        user.setEmail(null);
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setDeviceToken(null);
        user.setPasswordHash("DELETED_" + java.util.UUID.randomUUID().toString());
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean checkUserExistsByPhone(String phone) {
        String cleanPhone = phone.replaceAll("\\D", "");
        return userRepository.findAll().stream()
                .anyMatch(u -> {
                    if (u.getContact() == null)
                        return false;
                    String c = u.getContact().replaceAll("\\D", "");
                    return c.endsWith(
                            cleanPhone.length() >= 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone);
                });
    }

    public User loginByPhone(String idToken, String restaurantId, String providedName) {
        String phone;
        try {
            // 1. Mandatory Backend Verification (with Guest OTP Bypass)
            if ("guest-otp-bypass".equals(idToken)) {
                // SPECIAL BYPASS FOR DINE-IN (UPI-FIRST FLOW)
                // In this case, 'providedName' is used as the phone number passed from controller
                phone = providedName;
                System.out.println("[AuthService] Guest OTP Bypass triggered for: " + phone);
            } else {
                phone = firebaseService.verifyIdToken(idToken);
                System.out.println("[AuthService] Verified ID Token for: " + phone);
            }
        } catch (Exception e) {
            throw new RuntimeException("Authentication Failed: " + e.getMessage());
        }

        // 2. Clean phone number for normalized matching
        String cleanPhone = phone.replaceAll("\\D", "");
        String formattedPhone = phone.startsWith("+") ? phone : "+" + (cleanPhone.length() == 10 ? "91" + cleanPhone : cleanPhone);

        // 3. Try finding existing user
        Optional<User> userOpt = Optional.empty();
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            userOpt = userRepository.findFirstByContactAndRestaurantId(formattedPhone, restaurantId.toLowerCase().trim());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByContact(formattedPhone);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getContact() != null && u.getContact().replaceAll("\\D", "").endsWith(
                            cleanPhone.length() >= 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone))
                    .findFirst();
        }

        if (userOpt.isPresent()) {
            User user = assignSuperAdminIfApplicable(userOpt.get(), cleanPhone);
            // BYPASS subscription check for Dine-In sessions
            if (restaurantId == null || restaurantId.trim().isEmpty() || "guest-otp-bypass".equals(idToken)) {
                if ("GUEST".equalsIgnoreCase(user.getRole()) && !"guest-otp-bypass".equals(idToken)) {
                    System.out.println("[AuthService] Upgrading existing GUEST to ADMIN role for merchant login: " + phone);
                    user.setRole("ADMIN");
                    userRepository.save(user);
                }
                // Subscription check only for merchant login
                if (!"guest-otp-bypass".equals(idToken)) {
                    checkSubscription(user);
                }
            } else {
                checkSubscription(user);
            }
            return user;
        }

        // 4. Create New User
        if ((restaurantId != null && !restaurantId.trim().isEmpty()) || "guest-otp-bypass".equals(idToken)) {
            // DINE-IN GUEST FLOW
            System.out.println("[AuthService] Creating Guest record for Dine-In flow: " + phone);
            User guest = new User();
            guest.setUsername("customer_" + cleanPhone + "_" + System.currentTimeMillis());
            guest.setContact(formattedPhone);
            guest.setFirstName(providedName != null && !providedName.isEmpty() && !"guest-otp-bypass".equals(idToken) ? providedName : "Guest");
            guest.setRole("GUEST");
            guest.setRestaurantId(restaurantId != null ? restaurantId.toLowerCase().trim() : "unknown");
            guest.setPasswordHash(passwordEncoder.encode("GUEST_" + java.util.UUID.randomUUID().toString()));

            User savedUser = userRepository.save(guest);
            User finalUser = assignSuperAdminIfApplicable(savedUser, cleanPhone);

            // Save to Customers table
            try {
                if (customerRepository.findFirstByMobileAndRestaurantId(formattedPhone, guest.getRestaurantId()).isEmpty()) {
                    com.pos.server.model.Customer customer = new com.pos.server.model.Customer();
                    customer.setId("cust_" + cleanPhone + "_" + guest.getRestaurantId());
                    customer.setName(guest.getFirstName());
                    customer.setMobile(formattedPhone);
                    customer.setType("New");
                    customer.setRestaurantId(guest.getRestaurantId());
                    customer.setJoinedAt(java.time.OffsetDateTime.now().toString());
                    customerRepository.save(customer);
                }
            } catch (Exception e) {
                System.err.println("[AuthService] Failed to create customer record: " + e.getMessage());
            }
            
            return finalUser;
        } else {
            // AUTO-REGISTER ADMIN FLOW
            User newUser = new User();
            newUser.setUsername(formattedPhone);
            newUser.setContact(formattedPhone);
            newUser.setRole("ADMIN");
            newUser.setRestaurantId(formattedPhone); // Default to phone
            newUser.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

            User savedUser = userRepository.save(newUser);
            User finalUser = assignSuperAdminIfApplicable(savedUser, cleanPhone);
            checkSubscription(finalUser);
            return finalUser;
        }
    }

    public void checkSubscription(User user) {
        if (user == null) return;
        
        String contact = user.getContact() != null ? user.getContact() : "Unknown";
        String role = user.getRole() != null ? user.getRole() : "None";
        String rid = user.getRestaurantId();

        System.out.println("[Auth] Checking suspension for user: " + contact + " | Role: " + role + " | Restaurant: " + rid);

        // 1. Super Admin Role Bypass
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            System.out.println("[Auth] Bypassing: User has SUPER_ADMIN role");
            return;
        }

        // 2. Guest Role Bypass
        if ("GUEST".equalsIgnoreCase(role)) {
            System.out.println("[Auth] Bypassing: User has GUEST role");
            return;
        }

        // 3. Super Admin Phone Fallback
        if (contact != null && !contact.isEmpty()) {
            String cleanUPhone = contact.replaceAll("\\D", "");
            boolean isSaByNum = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .filter(p -> p.length() >= 10)
                    .anyMatch(p -> p.equals(cleanUPhone) || (cleanUPhone.length() >= 10 && cleanUPhone.endsWith(p)));
            if (isSaByNum) {
                System.out.println("[Auth] Bypassing: User phone " + contact + " is in Super Admin list");
                return;
            }
        }

        // 4. Actual Suspension Check
        if (rid != null && isRestaurantSuspended(rid)) {
            System.err.println("[Auth] BLOCKED: Restaurant " + rid + " is SUSPENDED for user: " + contact);
            throw new RuntimeException("Forbidden: Account is suspended");
        }
        
        System.out.println("[Auth] Check passed: Restaurant status is ACTIVE or not found");
    }

    public boolean isRestaurantSuspended(String restaurantId) {
        if (restaurantId == null)
            return false;
        Optional<Restaurant> restOpt = restaurantRepository.findById(restaurantId.toLowerCase());
        if (restOpt.isPresent()) {
            Restaurant r = restOpt.get();
            return "SUSPENDED".equalsIgnoreCase(r.getStatus());
        }
        return false;
    }

    public String validateRestaurantAccess(String headerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Unauthorized: Session not found");
        }

        String username = auth.getName();
        User user = findByUsername(username);

        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(user.getRole()) ||
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (isSuperAdmin) {
            String targetRid = (headerId != null && !headerId.trim().isEmpty()) ? headerId.toLowerCase().trim()
                    : user.getRestaurantId();
            return targetRid;
        }

        String userRid = user.getRestaurantId() != null ? user.getRestaurantId().toLowerCase() : null;
        String compareHeader = headerId != null ? headerId.toLowerCase() : null;
        
        if (compareHeader != null && !compareHeader.equals(userRid)) {
            // Also allow prefix/suffix matches: header "divya" should match stored ID "divya-1234"
            // This handles the case where QR codes/URLs use the short slug but the DB stores the full ID.
            boolean isPrefix = (userRid != null && (userRid.startsWith(compareHeader) || compareHeader.startsWith(userRid)));
            if (!isPrefix) {
                throw new RuntimeException("Access Denied: You do not have access to this restaurant's data");
            }
        }

        return userRid != null ? userRid : compareHeader;
    }

    public void validateTableAccess(String targetTableId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Unauthorized: Session not found");
        }

        String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String tokenTableId = jwtUtils.getTableIdFromToken(token);
            if (tokenTableId != null && targetTableId != null && !tokenTableId.trim().equalsIgnoreCase(targetTableId.trim())) {
                throw new RuntimeException("Access Denied: You do not have access to this table's session.");
            }
        }
    }


    @Transactional
    public Map<String, Object> submitOnboarding(User user, Map<String, String> data) {
        String businessName = data.get("businessName");
        String businessType = data.get("businessType");
        
        // Generate a unique restaurant ID (slug)
        String baseId = businessName.toLowerCase().trim().replaceAll("\\s+", "-");
        String restaurantId = baseId + "-" + (int)(Math.random() * 9000 + 1000);

        Restaurant r = new Restaurant();
        r.setId(restaurantId);
        r.setName(businessName);
        r.setStatus("PENDING");
        r.setBusinessType(businessType);
        restaurantRepository.save(r);

        user.setRestaurantId(restaurantId);
        user.setRole("ADMIN");
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("user", user);
        return response;
    }

    @Transactional
    public void updateDeviceToken(String username, String token) {
        User user = findByUsername(username);
        String oldToken = user.getDeviceToken();
        user.setDeviceToken(token);
        userRepository.save(user);
        System.out.println("[FORENSIC] [TOKEN-SYNC] User: " + username + " | RID: " + user.getRestaurantId() + " | Token: " + (token != null ? token.substring(0, 10) : "NULL") + "... | Previous: " + (oldToken != null ? "EXISTED" : "NONE"));
    }


    public List<String> findTokensByRestaurantIdSmart(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) return new java.util.ArrayList<>();
        
        String targetRid = restaurantId.trim().toLowerCase();
        String baseTargetRid = targetRid.contains(":") ? targetRid.split(":")[0] : targetRid;
        
        List<String> tokens = new java.util.ArrayList<>();
        List<User> allUsers = userRepository.findAll();
        
        for (User u : allUsers) {
            String uRid = (u.getRestaurantId() != null) ? u.getRestaurantId().trim().toLowerCase() : "";
            
            boolean isMatch = uRid.equals(targetRid) || uRid.equals(baseTargetRid);
            
            // Smart Mapping Fallback (Prefix/Suffix)
            if (!isMatch && !targetRid.isEmpty() && !uRid.isEmpty()) {
                isMatch = uRid.startsWith(targetRid) || targetRid.startsWith(uRid) ||
                          uRid.startsWith(baseTargetRid) || baseTargetRid.startsWith(uRid);
            }

            if (isMatch && u.getDeviceToken() != null && !u.getDeviceToken().isEmpty()) {
                tokens.add(u.getDeviceToken());
            }
        }
        return tokens;
    }

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
