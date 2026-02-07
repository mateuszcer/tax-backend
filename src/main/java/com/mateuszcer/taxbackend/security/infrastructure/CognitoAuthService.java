package com.mateuszcer.taxbackend.security.infrastructure;

import com.mateuszcer.taxbackend.security.CognitoService;
import com.mateuszcer.taxbackend.security.domain.AuthService;
import com.mateuszcer.taxbackend.security.domain.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Production authentication service using AWS Cognito.
 * Wraps the existing CognitoService to implement the AuthService interface.
 */
@Service
@Profile("!dev & !test & !simple-auth")
@Slf4j
public class CognitoAuthService implements AuthService {
    
    private final CognitoService cognitoService;
    
    public CognitoAuthService(CognitoService cognitoService) {
        this.cognitoService = cognitoService;
    }
    
    @Override
    public AuthResult signIn(String email, String password) {
        try {
            log.info("Authenticating user via AWS Cognito: {}", email);
            AuthenticationResultType authResult = cognitoService.signIn(email, password);
            
            if (authResult == null || authResult.idToken() == null) {
                throw new AuthenticationException("Authentication failed: No tokens received from Cognito");
            }
            
            log.info("User authenticated successfully via Cognito: {}", email);
            return new AuthResult(authResult.idToken(), authResult.accessToken());
            
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException e) {
            log.error("Cognito authentication failed for {}: {}", email, e.getMessage());
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during authentication for {}", email, e);
            throw new AuthenticationException("Authentication failed due to unexpected error", e);
        }
    }
    
    @Override
    public void signUp(String email, String password) {
        try {
            log.info("Registering user via AWS Cognito: {}", email);
            cognitoService.signUpUser(email, password);
            log.info("User registered successfully via Cognito: {}", email);
            
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException e) {
            log.error("Cognito signup failed for {}: {}", email, e.getMessage());
            throw new AuthenticationException("Signup failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during signup for {}", email, e);
            throw new AuthenticationException("Signup failed due to unexpected error", e);
        }
    }
    
    @Override
    public void confirmSignUp(String email, String confirmationCode) {
        try {
            log.info("Confirming user signup via AWS Cognito: {}", email);
            cognitoService.confirmSignUp(email, confirmationCode);
            log.info("User confirmed successfully via Cognito: {}", email);
            
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException e) {
            log.error("Cognito confirmation failed for {}: {}", email, e.getMessage());
            throw new AuthenticationException("Confirmation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during confirmation for {}", email, e);
            throw new AuthenticationException("Confirmation failed due to unexpected error", e);
        }
    }
}
