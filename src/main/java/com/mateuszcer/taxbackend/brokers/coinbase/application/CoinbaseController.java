package com.mateuszcer.taxbackend.brokers.coinbase.application;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.BrokerFacade;
import com.mateuszcer.taxbackend.brokers.domain.action.SaveBrokerAccessTokenAction;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOAuthUrlQuery;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOrdersQuery;
import com.mateuszcer.taxbackend.shared.authuserid.AuthUserId;
import com.mateuszcer.taxbackend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/broker/coinbase")
@Tag(name = "Coinbase Integration", description = "Coinbase broker integration operations")
@SecurityRequirement(name = "bearerAuth")
@Hidden
@Slf4j
public class CoinbaseController {

    private final BrokerFacade brokerFacade;

    public CoinbaseController(BrokerFacade brokerFacade) {
        this.brokerFacade = brokerFacade;
    }

    @GetMapping("/auth")
    @Operation(summary = "Initiate Coinbase OAuth", description = "Redirects user to Coinbase OAuth authorization")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Redirect to Coinbase OAuth authorization page",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public void initiateAuth(HttpServletResponse response) throws IOException {
        log.info("Initiating Coinbase OAuth flow");
        String url = brokerFacade.handle(new GetBrokerOAuthUrlQuery(Broker.COINBASE));
        response.sendRedirect(url);
    }

    @GetMapping("/auth/url")
    @Operation(summary = "Get Coinbase OAuth URL", description = "Returns the Coinbase OAuth authorization URL")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OAuth URL generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<AuthUrlResponse>> getAuthUrl() {
        log.info("Requesting Coinbase OAuth URL");
        String url = brokerFacade.handle(new GetBrokerOAuthUrlQuery(Broker.COINBASE));
        return ResponseEntity.ok(ApiResponse.success(new AuthUrlResponse(url), "OAuth URL generated successfully"));
    }

    @GetMapping("/auth/callback")
    @Operation(summary = "OAuth callback (GET)", description = "Handles OAuth callback with code as query parameter")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Coinbase integration successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Integration failed or missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<String>> oauthCallbackGet(
            @RequestParam String code,
            @AuthUserId String authUserId) {
        
        log.info("Processing OAuth callback (GET) for user: {}", authUserId);
        
        boolean saved = brokerFacade.handle(new SaveBrokerAccessTokenAction(Broker.COINBASE, code, authUserId));
        if (saved) {
            log.info("Successfully saved Coinbase access token for user: {}", authUserId);
            return ResponseEntity.ok(ApiResponse.success("Successfully integrated with Coinbase"));
        } else {
            log.warn("Failed to save Coinbase access token for user: {}", authUserId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Integration failed. Please try again.", "COINBASE_INTEGRATION_FAILED"));
        }
    }

    @PostMapping("/auth/exchange")
    @Operation(summary = "Exchange OAuth code (POST)", description = "Exchanges OAuth authorization code for Coinbase access tokens")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Coinbase integration successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Integration failed or missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Missing/invalid OAuth code parameter",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<String>> exchangeCodePost(
            @RequestBody ExchangeCodeRequest request,
            @AuthUserId String authUserId) {
        
        log.info("Exchanging OAuth code (POST) for user: {}", authUserId);
        
        boolean saved = brokerFacade.handle(new SaveBrokerAccessTokenAction(Broker.COINBASE, request.code(), authUserId));
        if (saved) {
            log.info("Successfully saved Coinbase access token for user: {}", authUserId);
            return ResponseEntity.ok(ApiResponse.success("Successfully integrated with Coinbase"));
        } else {
            log.warn("Failed to save Coinbase access token for user: {}", authUserId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Integration failed. Please try again.", "COINBASE_INTEGRATION_FAILED"));
        }
    }

    @GetMapping("/orders")
    @Operation(summary = "Get Coinbase orders", description = "Retrieves user's Coinbase orders")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT or Coinbase not integrated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<CoinbaseGetOrdersResponse>> getOrders(@AuthUserId String authUserId) {
        log.info("Fetching Coinbase orders for user: {}", authUserId);
        
        ActionResult<?> result = brokerFacade.handle(new GetBrokerOrdersQuery(Broker.COINBASE, authUserId));
        
        if (result.isSuccess()) {
            log.info("Successfully retrieved Coinbase orders for user: {}", authUserId);
            return ResponseEntity.ok(ApiResponse.success((CoinbaseGetOrdersResponse) result.getData(), "Orders retrieved successfully"));
        } else {
            log.warn("Failed to retrieve Coinbase orders for user: {}: {}", authUserId, result.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(result.getMessage(), "COINBASE_ORDERS_FAILED"));
        }
    }

    @Schema(name = "CoinbaseAuthUrlResponse", description = "Response containing the Coinbase OAuth authorization URL")
    public record AuthUrlResponse(
            @Schema(description = "Coinbase OAuth URL to redirect the user to", example = "https://www.coinbase.com/oauth/authorize?...") 
            String authUrl
    ) {}

    @Schema(name = "CoinbaseExchangeCodeRequest", description = "Request to exchange OAuth authorization code")
    public record ExchangeCodeRequest(
            @Schema(description = "OAuth authorization code from Coinbase", required = true, example = "abc123xyz...") 
            String code
    ) {}
}
