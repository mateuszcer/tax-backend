package com.mateuszcer.taxbackend.brokers.coinbase.adapter;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthOrdersBroker;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CoinbaseBrokerAdapter implements BrokerAdapter {

    private final OAuthOrdersBroker<CoinbaseGetOrdersResponse> oauthOrdersBroker;

    public CoinbaseBrokerAdapter(OAuthOrdersBroker<CoinbaseGetOrdersResponse> oauthOrdersBroker) {
        this.oauthOrdersBroker = oauthOrdersBroker;
    }

    @Override
    public Broker broker() {
        return Broker.COINBASE;
    }

    @Override
    public String getOAuthUrl() {
        return oauthOrdersBroker.getOAuthUrl();
    }

    @Override
    public boolean saveAccessToken(String code, String userId) {
        return oauthOrdersBroker.saveAccessToken(code, userId);
    }

    @Override
    public ActionResult<CoinbaseGetOrdersResponse> getOrders(String userId) {
        return oauthOrdersBroker.getOrders(userId);
    }

    @Override
    public ActionResult<List<NewOrdersEvent.OrderPayload>> getOrdersPayload(String userId) {
        ActionResult<CoinbaseGetOrdersResponse> result = oauthOrdersBroker.getOrders(userId);
        if (!result.isSuccess()) {
            return ActionResult.failure(result.getMessage());
        }

        CoinbaseGetOrdersResponse data = result.getData();
        if (data == null || data.getOrders() == null) {
            return ActionResult.success(List.of());
        }

        List<NewOrdersEvent.OrderPayload> payload = data.getOrders().stream()
                .filter(o -> o != null)
                .map(o -> new NewOrdersEvent.OrderPayload(
                        o.getOrderId(),
                        o.getProductId(),
                        o.getSide(),
                        o.getStatus(),
                        parseInstant(o.getLastFillTime(), o.getCreatedTime()),
                        parseDecimal(o.getFilledSize(), null),
                        parseDecimal(o.getAverageFilledPrice(), null),
                        parseDecimal(o.getTotalFees(), o.getFee()),
                        parseDecimal(o.getFilledValue(), null)
                ))
                .toList();

        return ActionResult.success(payload);
    }

    private static Instant parseInstant(String primary, String fallback) {
        String v = primary != null && !primary.isBlank() ? primary : fallback;
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String primary, String fallback) {
        String v = primary != null && !primary.isBlank() ? primary : fallback;
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return null;
        }
    }
}
