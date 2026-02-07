package com.mateuszcer.taxbackend.security.domain;

/**
 * Domain service interface for authentication operations.
 * Abstracts the authentication provider (Cognito in prod, mock in dev).
 */
public interface AuthService {
    
    /**
     * Authenticate user and return tokens.
     * 
     * @param email User email
     * @param password User password
     * @return Authentication result with tokens
     * @throws AuthenticationException if authentication fails
     */
    AuthResult signIn(String email, String password);
    
    /**
     * Register a new user.
     * 
     * @param email User email
     * @param password User password
     * @throws AuthenticationException if signup fails
     */
    void signUp(String email, String password);
    
    /**
     * Confirm user signup with verification code.
     * 
     * @param email User email
     * @param confirmationCode Verification code
     * @throws AuthenticationException if confirmation fails
     */
    void confirmSignUp(String email, String confirmationCode);
    
    /**
     * Authentication result containing tokens.
     */
    record AuthResult(String idToken, String accessToken) {}
}
