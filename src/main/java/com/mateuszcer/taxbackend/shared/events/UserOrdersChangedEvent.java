package com.mateuszcer.taxbackend.shared.events;

import java.util.List;

public record UserOrdersChangedEvent(String userId, List<Integer> taxYears) {
}


