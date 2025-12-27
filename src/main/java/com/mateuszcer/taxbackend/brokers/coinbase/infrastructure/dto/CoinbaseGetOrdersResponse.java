package com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CoinbaseGetOrdersResponse {
    private List<Order> orders;
    private String sequence;
    private boolean hasNext;
    private String cursor;

    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Order {
        private String orderId;
        private String productId;
        private String userId;
        private OrderConfiguration orderConfiguration;
        private String side;
        private String clientOrderId;
        private String status;
        private String timeInForce;
        private String createdTime;
        private String completionPercentage;
        private String filledSize;
        private String averageFilledPrice;
        private String fee;
        private String numberOfFills;
        private String filledValue;
        private boolean pendingCancel;
        private boolean sizeInQuote;
        private String totalFees;
        private boolean sizeInclusiveOfFees;
        private String totalValueAfterFees;
        private String triggerStatus;
        private String orderType;
        private String rejectReason;
        private boolean settled;
        private String productType;
        private String rejectMessage;
        private String cancelMessage;
        private String orderPlacementSource;
        private String outstandingHoldAmount;
        private boolean isLiquidation;
        private String lastFillTime;
        private List<EditHistory> editHistory;
        private String leverage;
        private String marginType;
        private String retailPortfolioId;
        private String originatingOrderId;
        private String attachedOrderId;
        private Object attachedOrderConfiguration;
    }

    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OrderConfiguration {
        private LimitLimitGtc limitLimitGtc;
    }

    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LimitLimitGtc {
        private String baseSize;
        private String limitPrice;
        private boolean postOnly;
    }

    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class EditHistory {
        private String price;
        private String size;
        private String replaceAcceptTimestamp;
    }
}

