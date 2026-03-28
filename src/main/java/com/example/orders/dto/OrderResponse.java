package com.example.orders.dto;

import com.example.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String product,
        Integer quantity,
        BigDecimal price,
        OrderStatus status
) {
}