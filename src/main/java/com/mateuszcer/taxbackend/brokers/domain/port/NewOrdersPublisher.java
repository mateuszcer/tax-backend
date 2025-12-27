package com.mateuszcer.taxbackend.brokers.domain.port;

import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;

public interface NewOrdersPublisher {
    void publish(NewOrdersEvent event);
}


