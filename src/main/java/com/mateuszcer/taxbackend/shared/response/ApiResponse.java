package com.mateuszcer.taxbackend.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "ApiResponse",
        description = "Standard response envelope used by all endpoints. On success `success=true` and `data` may be present; on error `success=false` and `errorCode` may be present."
)
public class ApiResponse<T> {
    
    @Schema(description = "Indicates whether the request was successful", example = "true")
    private final boolean success;
    
    @Schema(description = "Human-readable message describing the result", example = "User signed in successfully")
    private final String message;
    
    @Schema(description = "Response payload (present on success)")
    private final T data;
    
    @Schema(description = "Machine-readable error code (present on error)", example = "VALIDATION_ERROR")
    private final String errorCode;
    
    @Schema(description = "Response timestamp (server time)", example = "2025-12-26T12:34:56.789")
    private final LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
