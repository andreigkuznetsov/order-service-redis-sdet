package com.example.orders.service;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.dto.UpdateOrderStatusRequest;
import com.example.orders.entity.OrderEntity;
import com.example.orders.entity.OrderStatus;
import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.mapper.OrderMapper;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderCacheService orderCacheService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var savedId = idempotencyService.getSavedOrderId(idempotencyKey);
            if (savedId.isPresent()) {
                return getOrder(UUID.fromString(savedId.get()));
            }
        }

        Instant now = Instant.now();
        OrderEntity entity = OrderEntity.builder()
                .id(UUID.randomUUID())
                .product(request.product())
                .quantity(request.quantity())
                .price(request.price())
                .status(OrderStatus.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        OrderEntity saved = orderRepository.save(entity);
        OrderResponse response = orderMapper.toResponse(saved);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, saved.getId().toString());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return orderCacheService.get(id)
                .orElseGet(() -> {
                    OrderEntity entity = orderRepository.findById(id)
                            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
                    OrderResponse response = orderMapper.toResponse(entity);
                    orderCacheService.put(response);
                    return response;
                });
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, UpdateOrderStatusRequest request) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        entity.setStatus(request.status());
        entity.setUpdatedAt(Instant.now());

        OrderEntity saved = orderRepository.save(entity);
        orderCacheService.evict(id);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void setStatus(UUID id, OrderStatus status) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
        orderRepository.save(entity);
        orderCacheService.evict(id);
    }
}
