package com.mateuszcer.taxbackend.brokers.application;

import com.mateuszcer.taxbackend.brokers.domain.BrokerFacade;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import com.mateuszcer.taxbackend.brokers.domain.port.NewOrdersPublisher;
import com.mateuszcer.taxbackend.brokers.domain.usecase.SyncBrokerOrders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationEventPublisher;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Configuration
public class BrokersWiringConfig {

    @Bean
    public NewOrdersPublisher newOrdersPublisher(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    public SyncBrokerOrders syncBrokerOrders(NewOrdersPublisher newOrdersPublisher) {
        return new SyncBrokerOrders(newOrdersPublisher);
    }

    @Bean
    public BrokerFacade brokerFacade(List<BrokerAdapter> adapters, SyncBrokerOrders syncBrokerOrders) {
        Map<Broker, BrokerAdapter> map = new EnumMap<>(Broker.class);
        for (BrokerAdapter adapter : adapters) {
            map.put(adapter.broker(), adapter);
        }
        return new BrokerFacade(map, syncBrokerOrders);
    }
}
