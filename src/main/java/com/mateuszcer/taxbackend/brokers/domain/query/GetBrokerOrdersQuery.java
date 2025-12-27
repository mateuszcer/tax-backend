package com.mateuszcer.taxbackend.brokers.domain.query;

import com.mateuszcer.taxbackend.brokers.domain.Broker;

public record GetBrokerOrdersQuery(Broker broker, String userId) {
}


