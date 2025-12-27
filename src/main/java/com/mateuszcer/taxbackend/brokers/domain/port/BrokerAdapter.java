package com.mateuszcer.taxbackend.brokers.domain.port;

import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.Broker;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;

import java.util.List;

public interface BrokerAdapter {

    Broker broker();

    String getOAuthUrl();

    boolean saveAccessToken(String code, String userId);

    ActionResult<?> getOrders(String userId);

    ActionResult<List<NewOrdersEvent.OrderPayload>> getOrdersPayload(String userId);
}
