package com.example.orders.mapper;

import com.example.orders.dto.OrderResponse;
import com.example.orders.entity.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getProduct(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getStatus()
        );
    }
}
