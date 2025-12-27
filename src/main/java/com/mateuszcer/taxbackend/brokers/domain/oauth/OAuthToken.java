package com.mateuszcer.taxbackend.brokers.domain.oauth;

public record OAuthToken(String accessToken, String refreshToken, int expiresIn) {
}
