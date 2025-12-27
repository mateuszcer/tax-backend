package com.mateuszcer.taxbackend.security.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserConfirmSignUpRequest(
        @Schema(description = "User email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @Schema(description = "Confirmation code sent by email (6 digits)", example = "123456")
        @NotBlank(message = "Confirmation code is required")
        @Size(min = 6, max = 6, message = "Confirmation code must be 6 digits")
        String code
) {}