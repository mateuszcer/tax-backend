package com.mateuszcer.taxbackend.brokers.application;

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
class BrokerControllerTest {

    @Mock
    private BrokerFacade brokerFacade;

    @InjectMocks
    private BrokerController brokerController;

    @Test
    void getAuthUrl_ReturnsOAuthUrl() {
        String redirectUrl = "https://broker.example.com/oauth/authorize?client_id=test";
        when(brokerFacade.handle(any(GetBrokerOAuthUrlQuery.class))).thenReturn(redirectUrl);

        ResponseEntity response = brokerController.getAuthUrl("coinbase");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void oauthCallback_Success_Returns200() {
        when(brokerFacade.handle(any(SaveBrokerAccessTokenAction.class))).thenReturn(true);

        ResponseEntity response = brokerController.oauthCallback("coinbase", "code", "user");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getOrders_Success_Returns200() {
        ActionResult<?> success = ActionResult.success(new Object());
        when(brokerFacade.handle(any(GetBrokerOrdersQuery.class))).thenReturn((ActionResult) success);

        ResponseEntity response = brokerController.getOrders(Broker.COINBASE.name(), "user");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getOrders_Failure_Returns401() {
        when(brokerFacade.handle(any(GetBrokerOrdersQuery.class))).thenReturn(ActionResult.failure("No token"));

        ResponseEntity response = brokerController.getOrders("coinbase", "user");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}


