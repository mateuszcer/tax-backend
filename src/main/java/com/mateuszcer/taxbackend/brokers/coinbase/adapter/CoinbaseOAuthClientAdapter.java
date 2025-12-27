package com.mateuszcer.taxbackend.brokers.coinbase.adapter;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.CoinbaseClient;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.TokenResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthClient;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthToken;

public class CoinbaseOAuthClientAdapter implements OAuthClient<CoinbaseGetOrdersResponse> {

    private final CoinbaseClient coinbaseClient;

    public CoinbaseOAuthClientAdapter(CoinbaseClient coinbaseClient) {
        this.coinbaseClient = coinbaseClient;
    }

    @Override
    public String getAuthorizationUrl() {
        return coinbaseClient.getRedirectUrl();
    }

    @Override
    public OAuthToken exchangeCode(String code) {
        TokenResponse resp = coinbaseClient.getAccessToken(code);
        if (resp == null) return null;
        return new OAuthToken(resp.access_token(), resp.refresh_token(), resp.expires_in());
    }

    @Override
    public OAuthToken refresh(String refreshToken) {
        TokenResponse resp = coinbaseClient.refreshAccessToken(refreshToken);
        if (resp == null) return null;
        return new OAuthToken(resp.access_token(), resp.refresh_token(), resp.expires_in());
    }

    @Override
    public CoinbaseGetOrdersResponse fetchOrders(String accessToken) {
        return coinbaseClient.getOrders(accessToken);
    }
}


