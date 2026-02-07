package com.mateuszcer.taxbackend.security.application;

import com.mateuszcer.taxbackend.security.domain.AuthService;
import com.mateuszcer.taxbackend.security.application.dto.UserConfirmSignUpRequest;
import com.mateuszcer.taxbackend.security.application.dto.UserSignInRequest;
import com.mateuszcer.taxbackend.security.application.dto.UserSignUpRequest;
import com.mateuszcer.taxbackend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication operations")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signUp")
    @Operation(summary = "Sign up a new user", description = "Creates a new user account")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<String>> signUp(@Valid @RequestBody UserSignUpRequest userSignUpRequest) {
        log.info("User sign up attempt for email: {}", userSignUpRequest.email());
        
        authService.signUp(userSignUpRequest.email(), userSignUpRequest.password());
        
        log.info("User signed up successfully: {}", userSignUpRequest.email());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully. Please check your email for confirmation code."));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm user sign up", description = "Confirms user email with verification code")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User confirmed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or confirmation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<String>> confirmSignUp(@Valid @RequestBody UserConfirmSignUpRequest userConfirmSignUpRequest) {
        log.info("User confirmation attempt for email: {}", userConfirmSignUpRequest.email());
        
        authService.confirmSignUp(userConfirmSignUpRequest.email(), userConfirmSignUpRequest.code());
        
        log.info("User confirmed successfully: {}", userConfirmSignUpRequest.email());
        return ResponseEntity.ok(ApiResponse.success("User confirmed successfully. You can now sign in."));
    }

    @PostMapping("/signIn")
    @Operation(summary = "Sign in user", description = "Authenticates user and returns JWT token")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User signed in successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody UserSignInRequest userSignInRequest) {
        log.info("User sign in attempt for email: {}", userSignInRequest.email());
        
        var authResult = authService.signIn(userSignInRequest.email(), userSignInRequest.password());
        var response = new AuthResponse(authResult.idToken(), authResult.accessToken());
        
        log.info("User signed in successfully: {}", userSignInRequest.email());
        return ResponseEntity.ok(ApiResponse.success(response, "User signed in successfully"));
    }

    @Schema(name = "AuthResponse", description = "Authentication tokens returned after successful sign-in")
    public record AuthResponse(
            @Schema(description = "JWT ID token", example = "eyJraWQiOiJrZXktaWQiLCJhbGciOiJSUzI1NiJ9...")
            String idToken,
            @Schema(description = "JWT access token", example = "eyJraWQiOiJrZXktaWQiLCJhbGciOiJSUzI1NiJ9...")
            String accessToken
    ) {}
}
