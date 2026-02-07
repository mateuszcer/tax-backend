package com.mateuszcer.taxbackend.security.infrastructure;

import com.mateuszcer.taxbackend.security.domain.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Test-only authentication service.
 * Similar to DevAuthService but for test profile.
 */
@Service
@Profile("test")
@Slf4j
public class TestAuthService implements AuthService {
    
    @Override
    public AuthResult signIn(String email, String password) {
        log.debug("TEST MODE: Auto-approving login for email: {}", email);
        
        String fakeToken = generateFakeJwt(email);
        return new AuthResult(fakeToken, fakeToken);
    }
    
    @Override
    public void signUp(String email, String password) {
        log.debug("TEST MODE: Auto-approved signup for email: {}", email);
    }
    
    @Override
    public void confirmSignUp(String email, String confirmationCode) {
        log.debug("TEST MODE: Auto-confirmed signup for email: {}", email);
    }
    
    private String generateFakeJwt(String email) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        
        String userId = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String payload = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId,
                email,
                System.currentTimeMillis() / 1000,
                (System.currentTimeMillis() / 1000) + 86400
        );
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("test-signature-" + UUID.randomUUID()).getBytes());
        
        return header + "." + encodedPayload + "." + signature;
    }
}
