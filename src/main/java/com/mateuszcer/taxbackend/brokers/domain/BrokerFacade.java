package com.mateuszcer.taxbackend.brokers.domain;

import com.mateuszcer.taxbackend.brokers.domain.action.SaveBrokerAccessTokenAction;
import com.mateuszcer.taxbackend.brokers.domain.action.SyncBrokerOrdersAction;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOAuthUrlQuery;
import com.mateuszcer.taxbackend.brokers.domain.query.GetBrokerOrdersQuery;
import com.mateuszcer.taxbackend.brokers.domain.usecase.SyncBrokerOrders;

import java.util.Map;

public class BrokerFacade {

    private final Map<Broker, BrokerAdapter> adapters;
    private final SyncBrokerOrders syncBrokerOrders;

    public BrokerFacade(Map<Broker, BrokerAdapter> adapters, SyncBrokerOrders syncBrokerOrders) {
        this.adapters = adapters;
        this.syncBrokerOrders = syncBrokerOrders;
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

    public ActionResult<Integer> handle(SyncBrokerOrdersAction action) {
        return syncBrokerOrders.execute(action, adapter(action.broker()));
    }

    private BrokerAdapter adapter(Broker broker) {
        BrokerAdapter adapter = adapters.get(broker);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported broker: " + broker);
        }
        return adapter;
    }
}
