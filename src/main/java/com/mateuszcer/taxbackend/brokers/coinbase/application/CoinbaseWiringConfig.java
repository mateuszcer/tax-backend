package com.mateuszcer.taxbackend.brokers.coinbase.application;

import com.mateuszcer.taxbackend.brokers.coinbase.adapter.CoinbaseBrokerAdapter;
import com.mateuszcer.taxbackend.brokers.coinbase.adapter.CoinbaseOAuthClientAdapter;
import com.mateuszcer.taxbackend.brokers.coinbase.adapter.CoinbaseTokenStoreAdapter;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.CoinbaseClient;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository.CoinbaseTokenRepository;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthOrdersBroker;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinbaseWiringConfig {

    @Bean
    public CoinbaseOAuthClientAdapter coinbaseOAuthClientAdapter(CoinbaseClient coinbaseClient) {
        return new CoinbaseOAuthClientAdapter(coinbaseClient);
    }

    @Bean
    public CoinbaseTokenStoreAdapter coinbaseTokenStoreAdapter(CoinbaseTokenRepository coinbaseTokenRepository) {
        return new CoinbaseTokenStoreAdapter(coinbaseTokenRepository);
    }

    @Bean
    public OAuthOrdersBroker<CoinbaseGetOrdersResponse> coinbaseOAuthOrdersBroker(
            CoinbaseOAuthClientAdapter oauthClient,
            CoinbaseTokenStoreAdapter tokenStore
    ) {
        return new OAuthOrdersBroker<>(oauthClient, tokenStore);
    }

    @Bean
    public BrokerAdapter coinbaseBrokerAdapter(OAuthOrdersBroker<CoinbaseGetOrdersResponse> oauthOrdersBroker) {
        return new CoinbaseBrokerAdapter(oauthOrdersBroker);
    }
}


