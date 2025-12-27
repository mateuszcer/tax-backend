package com.mateuszcer.taxbackend.brokers.coinbase.application.usecase;

import com.mateuszcer.taxbackend.brokers.coinbase.adapter.CoinbaseOAuthClientAdapter;
import com.mateuszcer.taxbackend.brokers.coinbase.adapter.CoinbaseTokenStoreAdapter;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.CoinbaseClient;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.TokenResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository.CoinbaseTokenRepository;
import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.oauth.OAuthOrdersBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinbaseUseCasesTest {

    @Mock
    private CoinbaseClient coinbaseClient;

    @Mock
    private CoinbaseTokenRepository coinbaseTokenRepository;

    private OAuthOrdersBroker<CoinbaseGetOrdersResponse> broker;

    @BeforeEach
    void setUp() {
        var oauthClient = new CoinbaseOAuthClientAdapter(coinbaseClient);
        var tokenStore = new CoinbaseTokenStoreAdapter(coinbaseTokenRepository);
        broker = new OAuthOrdersBroker<>(oauthClient, tokenStore);
    }

    @Test
    void getOAuthUrl_ReturnsValidUrl() {
        String expectedUrl = "https://coinbase.com/oauth/authorize?...";
        when(coinbaseClient.getRedirectUrl()).thenReturn(expectedUrl);

        String result = broker.getOAuthUrl();

        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    void saveAccessToken_ValidCode_SavesTokenSuccessfully() {
        String code = "oauth-code";
        String userId = "test-user-id";
        TokenResponse tokenResponse = new TokenResponse(
                "access-token", "Bearer", 3600, "refresh-token", "read"
        );

        when(coinbaseClient.getAccessToken(code)).thenReturn(tokenResponse);
        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        boolean result = broker.saveAccessToken(code, userId);

        assertThat(result).isTrue();

        ArgumentCaptor<CoinbaseToken> tokenCaptor = ArgumentCaptor.forClass(CoinbaseToken.class);
        verify(coinbaseTokenRepository).save(tokenCaptor.capture());
        CoinbaseToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getAccessToken()).isEqualTo("access-token");
        assertThat(savedToken.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(savedToken.getExpiresIn()).isEqualTo(3600);
        assertThat(savedToken.getUserId()).isEqualTo(userId);
    }

    @Test
    void saveAccessToken_NullTokenResponse_ReturnsFalse() {
        String code = "oauth-code";
        String userId = "test-user-id";
        when(coinbaseClient.getAccessToken(code)).thenReturn(null);

        boolean result = broker.saveAccessToken(code, userId);

        assertThat(result).isFalse();
        verify(coinbaseTokenRepository, never()).save(any());
    }

    @Test
    void getOrders_NoTokenFound_ReturnsFailure() {
        String userId = "test-user-id";
        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("No token found for user. Please authenticate with broker.");
    }

    @Test
    void getOrders_ValidToken_ReturnsSuccess() {
        String userId = "test-user-id";
        String accessToken = "valid-access-token";
        CoinbaseGetOrdersResponse orders = new CoinbaseGetOrdersResponse();

        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .userId(userId)
                .expiresIn(3600)
                .build();

        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));
        when(coinbaseClient.getOrders(accessToken)).thenReturn(orders);

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(orders);
    }

    @Test
    void getOrders_ExpiredTokenSuccessfulRefresh_ReturnsSuccess() {
        String userId = "test-user-id";
        String oldAccessToken = "expired-access-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        CoinbaseGetOrdersResponse orders = new CoinbaseGetOrdersResponse();

        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken(oldAccessToken)
                .refreshToken("old-refresh-token")
                .userId(userId)
                .expiresIn(3600)
                .build();

        TokenResponse newTokenResponse = new TokenResponse(
                newAccessToken, "Bearer", 7200, newRefreshToken, "read"
        );

        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));
        when(coinbaseClient.getOrders(oldAccessToken)).thenThrow(new RuntimeException("401 Unauthorized"));
        when(coinbaseClient.refreshAccessToken("old-refresh-token")).thenReturn(newTokenResponse);
        when(coinbaseClient.getOrders(newAccessToken)).thenReturn(orders);
        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(orders);

        ArgumentCaptor<CoinbaseToken> tokenCaptor = ArgumentCaptor.forClass(CoinbaseToken.class);
        verify(coinbaseTokenRepository).save(tokenCaptor.capture());
        CoinbaseToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(savedToken.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(savedToken.getExpiresIn()).isEqualTo(7200);
    }

    @Test
    void getOrders_ExpiredTokenFailedRefresh_ReturnsFailure() {
        String userId = "test-user-id";
        String accessToken = "expired-access-token";

        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .userId(userId)
                .expiresIn(3600)
                .build();

        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));
        when(coinbaseClient.getOrders(accessToken)).thenThrow(new RuntimeException("401 Unauthorized"));
        when(coinbaseClient.refreshAccessToken("refresh-token")).thenReturn(null);

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Token refresh failed. Please authenticate with broker.");
    }

    @Test
    void getOrders_RefreshTokenThrowsException_ReturnsFailure() {
        String userId = "test-user-id";
        String accessToken = "expired-access-token";

        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .userId(userId)
                .expiresIn(3600)
                .build();

        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));
        when(coinbaseClient.getOrders(accessToken)).thenThrow(new RuntimeException("401 Unauthorized"));
        when(coinbaseClient.refreshAccessToken("refresh-token")).thenThrow(new RuntimeException("Network error"));

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Token refresh failed: Network error");
    }

    @Test
    void getOrders_ClientThrowsException_ReturnsFailure() {
        String userId = "test-user-id";
        String accessToken = "valid-access-token";

        CoinbaseToken token = CoinbaseToken.builder()
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .userId(userId)
                .expiresIn(3600)
                .build();

        when(coinbaseTokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));
        when(coinbaseClient.getOrders(accessToken)).thenThrow(new RuntimeException("Network error"));
        when(coinbaseClient.refreshAccessToken("refresh-token")).thenReturn(null);

        ActionResult<CoinbaseGetOrdersResponse> result = broker.getOrders(userId);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Token refresh failed. Please authenticate with broker.");
    }
}


