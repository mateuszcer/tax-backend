package com.mateuszcer.taxbackend.brokers.application;

import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.BrokerFacade;
import com.mateuszcer.taxbackend.brokers.domain.action.SaveBrokerAccessTokenAction;
import com.mateuszcer.taxbackend.brokers.domain.action.SyncBrokerOrdersAction;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOAuthUrlQuery;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOrdersQuery;
import com.mateuszcer.taxbackend.shared.authuserid.AuthUserId;
import com.mateuszcer.taxbackend.shared.config.FrontendConfig;
import com.mateuszcer.taxbackend.shared.exception.BusinessException;
import com.mateuszcer.taxbackend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Locale;

@RestController
@RequestMapping("/api/broker/{brokerId}")
@Tag(name = "Brokers", description = "Generic broker integration endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class BrokerController {

    private final BrokerFacade brokerFacade;
    private final FrontendConfig frontendConfig;

    public BrokerController(BrokerFacade brokerFacade, FrontendConfig frontendConfig) {
        this.brokerFacade = brokerFacade;
        this.frontendConfig = frontendConfig;
    }

    @GetMapping("/auth")
    @Operation(summary = "Initiate broker OAuth", description = "Redirects user to broker OAuth authorization")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Redirect to broker OAuth authorization page",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public void initiateAuth(@PathVariable String brokerId, HttpServletResponse response) throws IOException {
        Broker id = parseBroker(brokerId);
        String url = brokerFacade.handle(new GetBrokerOAuthUrlQuery(id));
        response.sendRedirect(url);
    }

    @GetMapping("/auth/url")
    @Operation(summary = "Get broker OAuth URL", description = "Returns the broker OAuth authorization URL")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OAuth URL generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<AuthUrlResponse>> getAuthUrl(@PathVariable String brokerId) {
        Broker id = parseBroker(brokerId);
        String url = brokerFacade.handle(new GetBrokerOAuthUrlQuery(id));
        return ResponseEntity.ok(ApiResponse.success(new AuthUrlResponse(url), "OAuth URL generated successfully"));
    }

    @PostMapping("/auth/exchange")
    @Operation(summary = "Exchange OAuth code", description = "Exchanges OAuth authorization code for access tokens")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Code exchanged successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker or invalid code",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT or exchange failed",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<String>> exchangeCode(
            @PathVariable String brokerId,
            @RequestBody ExchangeCodeRequest request,
            @AuthUserId String authUserId) {
        
        log.info("Exchanging OAuth code for broker {} and user {}", brokerId, authUserId);
        
        try {
            Broker id = parseBroker(brokerId);
            boolean saved = brokerFacade.handle(new SaveBrokerAccessTokenAction(id, request.code(), authUserId));
            
            if (saved) {
                log.info("Successfully exchanged code and saved tokens for user {} with broker {}", authUserId, brokerId);
                return ResponseEntity.ok(ApiResponse.success("Successfully integrated with " + brokerId.toLowerCase(Locale.ROOT)));
            } else {
                log.warn("Failed to save access token for user {} with broker {}", authUserId, brokerId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Failed to exchange authorization code", "CODE_EXCHANGE_FAILED"));
            }
        } catch (BusinessException e) {
            log.warn("Invalid broker in code exchange: {}", brokerId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unsupported broker: " + brokerId, "UNSUPPORTED_BROKER"));
        } catch (Exception e) {
            log.error("Failed to exchange code for user {} with broker {}", authUserId, brokerId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Failed to exchange authorization code: " + e.getMessage(), "CODE_EXCHANGE_FAILED"));
        }
    }

    @GetMapping("/orders")
    @Operation(summary = "Get broker orders", description = "Retrieves user's orders from selected broker")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT or broker not integrated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Object>> getOrders(@PathVariable String brokerId, @AuthUserId String authUserId) {
        Broker id = parseBroker(brokerId);

        ActionResult<?> result = brokerFacade.handle(new GetBrokerOrdersQuery(id, authUserId));
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getData(), "Orders retrieved successfully"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(result.getMessage(), "BROKER_ORDERS_FAILED"));
    }

    @PostMapping("/orders/sync")
    @Operation(summary = "Sync broker orders", description = "Fetches user's orders from selected broker and publishes NewOrdersEvent")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders synced successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT or broker not integrated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<SyncOrdersResponse>> syncOrders(@PathVariable String brokerId, @AuthUserId String authUserId) {
        Broker id = parseBroker(brokerId);

        ActionResult<Integer> result = brokerFacade.handle(new SyncBrokerOrdersAction(id, authUserId));
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(new SyncOrdersResponse(result.getData()), "Orders synced successfully"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(result.getMessage(), "BROKER_ORDERS_SYNC_FAILED"));
    }

    @Schema(name = "BrokerAuthUrlResponse", description = "Response containing the broker OAuth authorization URL")
    public record AuthUrlResponse(
            @Schema(description = "Broker OAuth URL to redirect the user to", example = "https://broker.example.com/oauth/authorize?...") String authUrl
    ) {}

    @Schema(name = "BrokerExchangeCodeRequest", description = "Request to exchange OAuth authorization code")
    public record ExchangeCodeRequest(
            @Schema(description = "OAuth authorization code from broker", required = true, example = "abc123xyz...") String code
    ) {}

    @Schema(name = "BrokerSyncOrdersResponse", description = "Response containing the number of orders synced")
    public record SyncOrdersResponse(
            @Schema(description = "Number of orders published to the system", example = "42") Integer syncedOrders
    ) {}

    private Broker parseBroker(String brokerId) {
        try {
            return Broker.valueOf(brokerId.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessException("Unsupported broker: " + brokerId, "UNSUPPORTED_BROKER");
        }
    }
}
