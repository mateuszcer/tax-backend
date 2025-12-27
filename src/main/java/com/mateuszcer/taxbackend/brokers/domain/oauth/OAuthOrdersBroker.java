package com.mateuszcer.taxbackend.brokers.domain.oauth;

import com.mateuszcer.taxbackend.brokers.domain.ActionResult;

public class OAuthOrdersBroker<TOrders> {

    private final OAuthClient<TOrders> oauthClient;
    private final OAuthTokenStore tokenStore;

    public OAuthOrdersBroker(OAuthClient<TOrders> oauthClient, OAuthTokenStore tokenStore) {
        this.oauthClient = oauthClient;
        this.tokenStore = tokenStore;
    }

    public String getOAuthUrl() {
        return oauthClient.getAuthorizationUrl();
    }

    public boolean saveAccessToken(String code, String userId) {
        OAuthToken token = oauthClient.exchangeCode(code);
        if (token == null) {
            return false;
        }
        tokenStore.save(userId, token);
        return true;
    }

    public ActionResult<TOrders> getOrders(String userId) {
        var tokenOpt = tokenStore.findByUserId(userId);
        if (tokenOpt.isEmpty()) {
            return ActionResult.failure("No token found for user. Please authenticate with broker.");
        }

        OAuthToken token = tokenOpt.get();
        ActionResult<TOrders> firstTry = tryFetchOrders(token.accessToken());
        if (firstTry.isSuccess()) {
            return firstTry;
        }

        try {
            OAuthToken refreshed = oauthClient.refresh(token.refreshToken());
            if (refreshed == null) {
                return ActionResult.failure("Token refresh failed. Please authenticate with broker.");
            }

            tokenStore.save(userId, refreshed);
            return tryFetchOrders(refreshed.accessToken());
        } catch (Exception e) {
            return ActionResult.failure("Token refresh failed: " + e.getMessage());
        }
    }

    private ActionResult<TOrders> tryFetchOrders(String accessToken) {
        try {
            TOrders orders = oauthClient.fetchOrders(accessToken);
            if (orders == null) {
                return ActionResult.failure("Broker returned empty response.");
            }
            return ActionResult.success(orders);
        } catch (Exception e) {
            return ActionResult.failure("Unauthorized: " + e.getMessage());
        }
    }
}
