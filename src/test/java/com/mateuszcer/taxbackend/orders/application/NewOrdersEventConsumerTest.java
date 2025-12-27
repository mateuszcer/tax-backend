package com.mateuszcer.taxbackend.orders.application;

import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import com.mateuszcer.taxbackend.orders.infrastructure.OrderRepository;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class NewOrdersEventConsumerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void consumesEventAndPersistsOrdersIdempotently() {
        orderRepository.deleteAll();

        NewOrdersEvent event = new NewOrdersEvent(
                "user1",
                List.of(new NewOrdersEvent.OrderPayload(
                        "ext1",
                        "ONDO-USDC",
                        "BUY",
                        "FILLED",
                        Instant.parse("2024-05-18T07:26:14.215Z"),
                        new BigDecimal("1.23"),
                        new BigDecimal("0.50"),
                        new BigDecimal("0.01"),
                        new BigDecimal("0.61")
                ))
        );

        publisher.publishEvent(event);
        publisher.publishEvent(event);

        var all = orderRepository.findByUserIdOrderByOccurredAtDesc("user1");
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getExternalId()).isEqualTo("ext1");
    }
}


