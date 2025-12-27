package com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository;

import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CoinbaseTokenRepository extends CrudRepository<CoinbaseToken, Long> {

    Optional<CoinbaseToken> findByUserId(String userId);
}
