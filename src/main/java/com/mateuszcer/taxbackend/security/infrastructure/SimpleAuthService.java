package com.mateuszcer.taxbackend.security.infrastructure;

import com.mateuszcer.taxbackend.security.domain.AuthService;
import com.mateuszcer.taxbackend.security.domain.AuthenticationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Simple JWT-based authentication service using PostgreSQL.
 * No external dependencies (Cognito, etc.) - perfect for demos and small deployments.
 * 
 * Profile: simple-auth
 */
@Service
@Profile("simple-auth")
@Slf4j
public class SimpleAuthService implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSigningKey;
    
    public SimpleAuthService(
            UserRepository userRepository,
            @Value("${jwt.secret:change-this-secret-key-in-production-min-256-bits}") String jwtSecret
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        
        // Ensure key is at least 256 bits for HS256
        String paddedSecret = jwtSecret.length() < 32 
            ? (jwtSecret + "0".repeat(32 - jwtSecret.length())) 
            : jwtSecret;
        this.jwtSigningKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        
        log.info("✅ SimpleAuthService initialized (JWT-based auth with PostgreSQL)");
    }
    
    @Override
    @Transactional
    public AuthResult signIn(String email, String password) {
        log.info("Sign in attempt for email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        
        if (!user.isConfirmed()) {
            log.warn("Sign in failed - user not confirmed: {}", email);
            throw new AuthenticationException("Email not confirmed");
        }
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Sign in failed - invalid password for: {}", email);
            throw new AuthenticationException("Invalid email or password");
        }
        
        String token = generateJwt(user.getUserId(), user.getEmail());
        
        log.info("✅ User signed in successfully: {}", email);
        return new AuthResult(token, token);
    }
    
    @Override
    @Transactional
    public void signUp(String email, String password) {
        log.info("Sign up attempt for email: {}", email);
        
        if (userRepository.existsByEmail(email)) {
            log.warn("Sign up failed - email already exists: {}", email);
            throw new AuthenticationException("Email already registered");
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserId(UUID.randomUUID().toString());
        user.setConfirmed(true); // Auto-confirm for demo (no email verification)
        
        userRepository.save(user);
        
        log.info("✅ User registered successfully (auto-confirmed): {}", email);
    }
    
    @Override
    public void confirmSignUp(String email, String confirmationCode) {
        // No-op for simple auth - users are auto-confirmed on signup
        log.info("Confirmation request for {} (auto-confirmed, no action needed)", email);
    }
    
    /**
     * Generate JWT token with userId as subject and email as claim.
     */
    private String generateJwt(String userId, String email) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + 86400000); // 24 hours
        
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(jwtSigningKey)
            .compact();
    }
}
