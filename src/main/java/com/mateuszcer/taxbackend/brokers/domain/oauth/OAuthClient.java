package com.mateuszcer.taxbackend.brokers.domain.oauth;

public interface OAuthClient<TOrders> {
    String getAuthorizationUrl();

    OAuthToken exchangeCode(String code);

    OAuthToken refresh(String refreshToken);

    TOrders fetchOrders(String accessToken);
}
