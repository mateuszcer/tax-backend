package com.mateuszcer.taxbackend.capitalgains.domain.port;

import com.mateuszcer.taxbackend.capitalgains.domain.OrderSnapshot;

import java.util.List;

public interface UserOrdersProvider {
    List<OrderSnapshot> getForUser(String userId);
}


