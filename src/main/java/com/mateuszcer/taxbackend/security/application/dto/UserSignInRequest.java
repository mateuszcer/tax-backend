package com.mateuszcer.taxbackend.security.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserSignInRequest(
        @Schema(description = "User email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @Schema(description = "User password", example = "StrongPassword123!")
        @NotBlank(message = "Password is required")
        String password
) {}
