package com.mateuszcer.taxbackend.security.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * JWT authentication filter for simple-auth profile.
 * Validates JWT tokens and sets Spring Security authentication context.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final SecretKey jwtSigningKey;
    
    public JwtAuthenticationFilter(String jwtSecret) {
        String paddedSecret = jwtSecret.length() < 32 
            ? (jwtSecret + "0".repeat(32 - jwtSecret.length())) 
            : jwtSecret;
        this.jwtSigningKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                
                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                
                // Create Spring Security Jwt object (compatible with AuthUserIdResolver)
                Map<String, Object> headers = Map.of("alg", "HS256", "typ", "JWT");
                Map<String, Object> claimsMap = Map.of(
                    JwtClaimNames.SUB, userId,
                    "email", email,
                    JwtClaimNames.IAT, claims.getIssuedAt().toInstant(),
                    JwtClaimNames.EXP, claims.getExpiration().toInstant()
                );
                
                Jwt jwt = new Jwt(
                    token,
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant(),
                    headers,
                    claimsMap
                );
                
                // Set authentication in SecurityContext
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(jwt, null, Collections.emptyList());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authenticated: userId={}, email={}", userId, email);
                
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                // Don't set authentication - request will be unauthorized
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
