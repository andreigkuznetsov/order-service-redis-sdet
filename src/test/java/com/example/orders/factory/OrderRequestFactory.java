package com.example.orders.factory;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.UpdateOrderStatusRequest;
import com.example.orders.entity.OrderStatus;

import java.math.BigDecimal;

public class OrderRequestFactory {

    public static CreateOrderRequest validCreateOrderRequest() {
        return new CreateOrderRequest(
                "iPhone 15",
                1,
                new BigDecimal("999.99")
        );
    }

    public static CreateOrderRequest validCreateOrderRequest(String product) {
        return new CreateOrderRequest(
                product,
                1,
                new BigDecimal("999.99")
        );
    }

    public static UpdateOrderStatusRequest updateStatusRequest(OrderStatus status) {
        return new UpdateOrderStatusRequest(status);
    }
}