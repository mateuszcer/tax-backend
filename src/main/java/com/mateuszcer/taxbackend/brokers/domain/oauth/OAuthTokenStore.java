package com.mateuszcer.taxbackend.brokers.domain.oauth;

import java.util.Optional;

public interface OAuthTokenStore {
    Optional<OAuthToken> findByUserId(String userId);

    void save(String userId, OAuthToken token);
}
