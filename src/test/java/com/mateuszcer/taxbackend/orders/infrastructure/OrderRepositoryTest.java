package com.mateuszcer.taxbackend.orders.infrastructure;

import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import com.mateuszcer.taxbackend.orders.domain.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByUserIdAndExternalId_ReturnsOrder() {
        Order o = new Order();
        o.setUserId("user1");
        o.setExternalId("ext1");
        o.setProductId("ONDO-USDC");
        o.setSide("BUY");
        o.setStatus("FILLED");
        o.setOccurredAt(Instant.parse("2024-05-18T07:26:14.215Z"));
        entityManager.persistAndFlush(o);

        Optional<Order> found = orderRepository.findByUserIdAndExternalId("user1", "ext1");
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo("ONDO-USDC");
    }

    @Test
    void findByUserIdOrderByOccurredAtDesc_Sorts() {
        Order o1 = new Order();
        o1.setUserId("user1");
        o1.setExternalId("ext1");
        o1.setProductId("ONDO-USDC");
        o1.setSide("BUY");
        o1.setStatus("FILLED");
        o1.setOccurredAt(Instant.parse("2024-05-18T07:26:14.215Z"));

        Order o2 = new Order();
        o2.setUserId("user1");
        o2.setExternalId("ext2");
        o2.setProductId("ONDO-USDC");
        o2.setSide("SELL");
        o2.setStatus("FILLED");
        o2.setOccurredAt(Instant.parse("2024-04-30T11:08:33.174191Z"));

        entityManager.persistAndFlush(o2);
        entityManager.persistAndFlush(o1);

        List<Order> list = orderRepository.findByUserIdOrderByOccurredAtDesc("user1");
        assertThat(list).hasSize(2);
        assertThat(list.getFirst().getExternalId()).isEqualTo("ext1");
    }
}


