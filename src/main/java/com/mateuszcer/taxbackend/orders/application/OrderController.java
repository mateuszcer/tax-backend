package com.mateuszcer.taxbackend.orders.application;

import com.mateuszcer.taxbackend.orders.domain.Order;
import com.mateuszcer.taxbackend.orders.domain.OrderFacade;
import com.mateuszcer.taxbackend.orders.domain.query.GetUserOrdersQuery;
import com.mateuszcer.taxbackend.shared.authuserid.AuthUserId;
import com.mateuszcer.taxbackend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order domain")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @GetMapping
    @Operation(summary = "Get user orders", description = "Returns all orders for authenticated user")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@AuthUserId String authUserId) {
        List<OrderResponse> data = orderFacade.handle(new GetUserOrdersQuery(authUserId)).stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Orders retrieved successfully"));
    }

    @Schema(name = "Order")
    public record OrderResponse(
            @Schema(example = "123") Long id,
            @Schema(example = "b2953b47-ed65-425b-810d-2b46a076c3c1") String externalId,
            @Schema(example = "ONDO-USDC") String productId,
            @Schema(example = "SELL") String side,
            @Schema(example = "FILLED") String status,
            Instant occurredAt,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fee,
            BigDecimal total
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getId(),
                    order.getExternalId(),
                    order.getProductId(),
                    order.getSide(),
                    order.getStatus(),
                    order.getOccurredAt(),
                    order.getQuantity(),
                    order.getPrice(),
                    order.getFee(),
                    order.getTotal()
            );
        }
    }
}


