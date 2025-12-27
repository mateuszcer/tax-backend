package com.mateuszcer.taxbackend.brokers.coinbase.adapter;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository.CoinbaseTokenRepository;
import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthToken;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthTokenStore;

import java.util.Optional;

public class CoinbaseTokenStoreAdapter implements OAuthTokenStore {

    private final CoinbaseTokenRepository coinbaseTokenRepository;

    public CoinbaseTokenStoreAdapter(CoinbaseTokenRepository coinbaseTokenRepository) {
        this.coinbaseTokenRepository = coinbaseTokenRepository;
    }

    @Override
    public Optional<OAuthToken> findByUserId(String userId) {
        return coinbaseTokenRepository.findByUserId(userId)
                .map(t -> new OAuthToken(t.getAccessToken(), t.getRefreshToken(), t.getExpiresIn()));
    }

    @Override
    public void save(String userId, OAuthToken token) {
        Optional<CoinbaseToken> existing = coinbaseTokenRepository.findByUserId(userId);

        CoinbaseToken entity = existing.orElseGet(CoinbaseToken::new);
        entity.setUserId(userId);
        entity.setAccessToken(token.accessToken());
        entity.setRefreshToken(token.refreshToken());
        entity.setExpiresIn(token.expiresIn());

        coinbaseTokenRepository.save(entity);
    }
}
