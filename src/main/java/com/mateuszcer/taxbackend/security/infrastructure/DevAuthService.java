package com.mateuszcer.taxbackend.security.infrastructure;

import com.mateuszcer.taxbackend.security.domain.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Development-only authentication service.
 * Accepts any email/password combination and returns fake tokens.
 * 
 * DO NOT USE IN PRODUCTION!
 */
@Service
@Profile("dev")
@Slf4j
public class DevAuthService implements AuthService {
    
    @Override
    public AuthResult signIn(String email, String password) {
        log.info("🔓 DEV MODE: Auto-approving login for email: {}", email);
        log.info("🔓 DEV MODE: Password accepted (any password works in dev)");
        
        String fakeToken = generateFakeJwt(email);
        
        log.info("✅ DEV MODE: Generated fake token for {}", email);
        return new AuthResult(fakeToken, fakeToken);
    }
    
    @Override
    public void signUp(String email, String password) {
        log.info("🔓 DEV MODE: Auto-approved signup for email: {}", email);
        log.info("✅ DEV MODE: User {} registered (no real AWS call)", email);
    }
    
    @Override
    public void confirmSignUp(String email, String confirmationCode) {
        log.info("🔓 DEV MODE: Auto-confirmed signup for email: {} with code: {}", email, confirmationCode);
        log.info("✅ DEV MODE: User {} confirmed (no real AWS call)", email);
    }
    
    /**
     * Generate a fake JWT-like token for dev mode.
     * Format: header.payload.signature (all fake)
     */
    private String generateFakeJwt(String email) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        
        String userId = "dev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String payload = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId,
                email,
                System.currentTimeMillis() / 1000,
                (System.currentTimeMillis() / 1000) + 86400 // 24h expiry
        );
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("dev-signature-" + UUID.randomUUID()).getBytes());
        
        return header + "." + encodedPayload + "." + signature;
    }
}
