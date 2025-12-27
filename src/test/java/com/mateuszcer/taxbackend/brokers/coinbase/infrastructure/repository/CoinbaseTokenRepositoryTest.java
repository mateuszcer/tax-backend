package com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository;

import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CoinbaseTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CoinbaseTokenRepository coinbaseTokenRepository;

    @Test
    void findByUserId_ExistingUser_ReturnsToken() {
        String userId = "test-user-123";
        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .expiresIn(3600)
                .userId(userId)
                .build();
        
        entityManager.persistAndFlush(token);

        Optional<CoinbaseToken> result = coinbaseTokenRepository.findByUserId(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getAccessToken()).isEqualTo("access-token-123");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(result.get().getExpiresIn()).isEqualTo(3600);
    }

    @Test
    void findByUserId_NonExistingUser_ReturnsEmpty() {
        String userId = "non-existing-user";

        Optional<CoinbaseToken> result = coinbaseTokenRepository.findByUserId(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void save_NewToken_SavesSuccessfully() {
        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(7200)
                .userId("new-user-123")
                .build();

        CoinbaseToken savedToken = coinbaseTokenRepository.save(token);

        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getAccessToken()).isEqualTo("new-access-token");
        assertThat(savedToken.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedToken.getExpiresIn()).isEqualTo(7200);
        assertThat(savedToken.getUserId()).isEqualTo("new-user-123");
    }

    @Test
    void save_UpdateExistingToken_UpdatesSuccessfully() {
        String userId = "existing-user-123";
        CoinbaseToken originalToken = CoinbaseToken.builder()
                .accessToken("old-access-token")
                .refreshToken("old-refresh-token")
                .expiresIn(3600)
                .userId(userId)
                .build();
        
        CoinbaseToken savedToken = entityManager.persistAndFlush(originalToken);
        
        savedToken.setAccessToken("updated-access-token");
        savedToken.setRefreshToken("updated-refresh-token");
        savedToken.setExpiresIn(7200);

        CoinbaseToken updatedToken = coinbaseTokenRepository.save(savedToken);

        assertThat(updatedToken.getId()).isEqualTo(savedToken.getId());
        assertThat(updatedToken.getAccessToken()).isEqualTo("updated-access-token");
        assertThat(updatedToken.getRefreshToken()).isEqualTo("updated-refresh-token");
        assertThat(updatedToken.getExpiresIn()).isEqualTo(7200);
        assertThat(updatedToken.getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserId_MultipleUsersWithTokens_ReturnsCorrectToken() {
        CoinbaseToken token1 = CoinbaseToken.builder()
                .accessToken("token1")
                .refreshToken("refresh1")
                .expiresIn(3600)
                .userId("user1")
                .build();
        
        CoinbaseToken token2 = CoinbaseToken.builder()
                .accessToken("token2")
                .refreshToken("refresh2")
                .expiresIn(7200)
                .userId("user2")
                .build();
        
        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);

        Optional<CoinbaseToken> result1 = coinbaseTokenRepository.findByUserId("user1");
        Optional<CoinbaseToken> result2 = coinbaseTokenRepository.findByUserId("user2");

        assertThat(result1).isPresent();
        assertThat(result1.get().getAccessToken()).isEqualTo("token1");
        assertThat(result1.get().getUserId()).isEqualTo("user1");
        
        assertThat(result2).isPresent();
        assertThat(result2.get().getAccessToken()).isEqualTo("token2");
        assertThat(result2.get().getUserId()).isEqualTo("user2");
    }
}
