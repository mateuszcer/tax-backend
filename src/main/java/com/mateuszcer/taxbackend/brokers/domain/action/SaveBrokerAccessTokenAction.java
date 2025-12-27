package com.mateuszcer.taxbackend.brokers.domain.action;

import com.mateuszcer.taxbackend.brokers.domain.Broker;

public record SaveBrokerAccessTokenAction(Broker broker, String code, String userId) {
}


