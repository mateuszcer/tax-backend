package com.mateuszcer.taxbackend.brokers.domain;

import com.mateuszcer.taxbackend.brokers.domain.action.SaveBrokerAccessTokenAction;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOAuthUrlQuery;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOrdersQuery;

import java.util.Map;

public class BrokerFacade {

    private final Map<Broker, BrokerAdapter> adapters;

    public BrokerFacade(Map<Broker, BrokerAdapter> adapters) {
        this.adapters = adapters;
    }

    public String handle(GetBrokerOAuthUrlQuery query) {
        return adapter(query.broker()).getOAuthUrl();
    }

    public boolean handle(SaveBrokerAccessTokenAction action) {
        return adapter(action.broker()).saveAccessToken(action.code(), action.userId());
    }

    public ActionResult<?> handle(GetBrokerOrdersQuery query) {
        return adapter(query.broker()).getOrders(query.userId());
    }

    private BrokerAdapter adapter(Broker broker) {
        BrokerAdapter adapter = adapters.get(broker);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported broker: " + broker);
        }
        return adapter;
    }
}
