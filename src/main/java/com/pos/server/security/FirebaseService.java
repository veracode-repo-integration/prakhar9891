package com.pos.server.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    public String verifyIdToken(String idToken) throws Exception {
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance("PosFirebaseApp");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(app).verifyIdToken(idToken);
            String phone = (String) decodedToken.getClaims().get("phone_number");
            
            if (phone == null || phone.isEmpty()) {
                // Some Firebase providers (like email) don't have phone_number in claims by default
                // But for OTP flow it should be there.
                throw new Exception("No phone number found in token");
            }
            
            return phone;
        } catch (Exception e) {
            System.err.println("[Firebase] Token verification failed: " + e.getMessage());
            throw new Exception("Invalid OTP Token: " + e.getMessage());
        }
    }
}
