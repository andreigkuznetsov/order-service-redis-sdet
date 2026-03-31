package com.example.orders.support;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String orderCache(String orderId) {
        return "order:cache:" + orderId;
    }

    public static String orderLock(String orderId) {
        return "order:lock:" + orderId;
    }
}
