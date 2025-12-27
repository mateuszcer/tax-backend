package com.mateuszcer.taxbackend.brokers.domain.action;

import com.mateuszcer.taxbackend.brokers.domain.Broker;

public record SyncBrokerOrdersAction(Broker broker, String userId) {
}


