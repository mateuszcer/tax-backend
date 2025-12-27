package com.mateuszcer.taxbackend.brokers.domain;

import lombok.Getter;

@Getter
public class ActionResult<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ActionResult(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ActionResult<T> success(T data) {
        return new ActionResult<>(true, data, null);
    }

    public static <T> ActionResult<T> failure(String message) {
        return new ActionResult<>(false, null, message);
    }
}
