package com.mateuszcer.taxbackend.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ResourceNotFoundException userNotFound(String userId) {
        return new ResourceNotFoundException("User not found with ID: " + userId);
    }
    
    public static ResourceNotFoundException tokenNotFound(String userId) {
        return new ResourceNotFoundException("Token not found for user: " + userId);
    }
}
