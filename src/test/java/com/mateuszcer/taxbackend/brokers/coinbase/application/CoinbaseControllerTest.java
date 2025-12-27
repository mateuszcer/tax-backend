package com.mateuszcer.taxbackend.brokers.coinbase.application;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.BrokerFacade;
import com.mateuszcer.taxbackend.brokers.domain.action.SaveBrokerAccessTokenAction;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOAuthUrlQuery;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOrdersQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoinbaseControllerTest {

    @Mock
    private BrokerFacade brokerFacade;

    @InjectMocks
    private CoinbaseController coinbaseController;

    @Test
    void getAuthUrl_ReturnsOAuthUrl() {
        String redirectUrl = "https://coinbase.com/oauth/authorize?client_id=test&response_type=code";
        when(brokerFacade.handle(any(GetBrokerOAuthUrlQuery.class))).thenReturn(redirectUrl);

        ResponseEntity response = coinbaseController.getAuthUrl();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void oauthCallback_ValidCode_ReturnsSuccess() {
        String code = "oauth-authorization-code";
        String userId = "test-user-123";
        when(brokerFacade.handle(any(SaveBrokerAccessTokenAction.class))).thenReturn(true);

        ResponseEntity response = coinbaseController.oauthCallback(code, userId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void oauthCallback_InvalidCode_ReturnsUnauthorized() {
        String code = "invalid-code";
        String userId = "test-user-123";
        when(brokerFacade.handle(any(SaveBrokerAccessTokenAction.class))).thenReturn(false);

        ResponseEntity response = coinbaseController.oauthCallback(code, userId);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getOrders_ValidUser_ReturnsOrders() {
        String userId = "test-user-123";
        CoinbaseGetOrdersResponse orders = new CoinbaseGetOrdersResponse();
        ActionResult<?> successResult = ActionResult.success(orders);
        when(brokerFacade.handle(any(GetBrokerOrdersQuery.class))).thenReturn((ActionResult) successResult);

        ResponseEntity response = coinbaseController.getOrders(userId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getOrders_NoToken_ReturnsUnauthorized() {
        String userId = "test-user-123";
        ActionResult<?> failureResult = ActionResult.failure("No token found for user. Please authenticate with Coinbase.");
        when(brokerFacade.handle(any(GetBrokerOrdersQuery.class))).thenReturn((ActionResult) failureResult);

        ResponseEntity response = coinbaseController.getOrders(userId);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getOrders_ExpiredToken_ReturnsUnauthorized() {
        String userId = "test-user-123";
        ActionResult<?> failureResult = ActionResult.failure("Token refresh failed. Please authenticate with Coinbase.");
        when(brokerFacade.handle(any(GetBrokerOrdersQuery.class))).thenReturn((ActionResult) failureResult);

        ResponseEntity response = coinbaseController.getOrders(userId);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
    }
}
