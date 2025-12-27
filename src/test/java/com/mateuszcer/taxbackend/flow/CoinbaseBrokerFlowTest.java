package com.mateuszcer.taxbackend.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.CoinbaseClient;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository.CoinbaseTokenRepository;
import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import com.mateuszcer.taxbackend.capitalgains.infrastructure.CapitalGainsReportRepository;
import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import com.mateuszcer.taxbackend.orders.infrastructure.OrderRepository;
import com.mateuszcer.taxbackend.pit.infrastructure.PitReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CoinbaseBrokerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoinbaseTokenRepository coinbaseTokenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CapitalGainsReportRepository capitalGainsReportRepository;

    @Autowired
    private PitReportRepository pitReportRepository;

    @MockBean
    private CoinbaseClient coinbaseClient;

    @BeforeEach
    void setup() {
        pitReportRepository.deleteAll();
        capitalGainsReportRepository.deleteAll();
        orderRepository.deleteAll();
        coinbaseTokenRepository.deleteAll();
    }

    @Test
    void syncOrders_usesRealCoinbaseExampleJson_and_updatesOrdersCapitalGainsAndPit() throws Exception {
        String userId = "u1";
        String accessToken = "access-token";

        coinbaseTokenRepository.save(CoinbaseToken.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .expiresIn(3600)
                .build());

        CoinbaseGetOrdersResponse response = objectMapper.readValue(
                new ClassPathResource("coinbase/example-coinbase.json").getInputStream(),
                CoinbaseGetOrdersResponse.class
        );

        when(coinbaseClient.getOrders(accessToken)).thenReturn(response);

        mockMvc.perform(
                        post("/api/broker/coinbase/orders/sync")
                                .with(jwt().jwt(Jwt.withTokenValue("t")
                                        .claim("sub", userId)
                                        .issuedAt(Instant.now())
                                        .expiresAt(Instant.now().plusSeconds(3600))
                                        .header("alg", "none")
                                        .build()))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        assertThat(orderRepository.findByUserIdOrderByOccurredAtDesc(userId)).hasSize(2);

        var cg = capitalGainsReportRepository.findByUserIdAndTaxYear(userId, 2024);
        assertThat(cg).isPresent();
        assertThat(cg.get().getCost()).isEqualByComparingTo(new BigDecimal("238.48"));
        assertThat(cg.get().getProceeds()).isEqualByComparingTo(new BigDecimal("915.92"));
        assertThat(cg.get().getGain()).isEqualByComparingTo(new BigDecimal("677.44"));

        var pit = pitReportRepository.findByUserIdAndTaxYear(userId, 2024);
        assertThat(pit).isPresent();
        assertThat(pit.get().getGain()).isEqualByComparingTo(new BigDecimal("677.44"));
    }
}


