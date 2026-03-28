package com.example.orders.dto;

import java.util.UUID;

public record ProcessOrderResponse(
        String message,
        UUID orderId
) {
}
