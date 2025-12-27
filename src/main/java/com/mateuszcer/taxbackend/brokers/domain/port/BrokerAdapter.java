package com.mateuszcer.taxbackend.brokers.domain.port;

import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;

public interface BrokerAdapter {

    Broker broker();

    String getOAuthUrl();

    boolean saveAccessToken(String code, String userId);

    ActionResult<?> getOrders(String userId);
}
