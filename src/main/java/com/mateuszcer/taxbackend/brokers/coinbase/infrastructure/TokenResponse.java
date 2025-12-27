package com.mateuszcer.taxbackend.brokers.coinbase.infrastructure;

public record TokenResponse(String access_token, String token_type, int expires_in, String refresh_token, String scope) {
}
