package com.mateuszcer.taxbackend.brokers.coinbase.adapter;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthOrdersBroker;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;

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
}
