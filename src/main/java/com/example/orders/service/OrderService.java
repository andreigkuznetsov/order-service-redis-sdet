package com.example.orders.service;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.entity.OrderEntity;
import com.example.orders.entity.OrderStatus;
import com.example.orders.exception.IdempotencyProcessingException;
import com.example.orders.mapper.OrderMapper;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createNewOrder(request);
        }

        var existingValue = idempotencyService.getValue(idempotencyKey);

        if (existingValue.isPresent() && !idempotencyService.isProcessingValue(existingValue.get())) {
            return getOrder(UUID.fromString(existingValue.get()));
        }

        boolean acquired = idempotencyService.tryAcquireProcessing(idempotencyKey);

        if (!acquired) {
            return waitForExistingOrder(idempotencyKey);
        }

        try {
            OrderResponse response = createNewOrder(request);
            idempotencyService.saveOrderId(idempotencyKey, response.id().toString());
            return response;
        } catch (RuntimeException ex) {
            idempotencyService.remove(idempotencyKey);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return orderCacheService.get(id)
                .orElseGet(() -> {
                    OrderEntity entity = orderRepository.findById(id)
                            .orElseThrow(() -> new com.example.orders.exception.OrderNotFoundException("Order not found: " + id));
                    OrderResponse response = orderMapper.toResponse(entity);
                    orderCacheService.put(response);
                    return response;
                });
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, com.example.orders.dto.UpdateOrderStatusRequest request) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new com.example.orders.exception.OrderNotFoundException("Order not found: " + id));

        entity.setStatus(request.status());
        entity.setUpdatedAt(Instant.now());

        OrderEntity saved = orderRepository.save(entity);
        orderCacheService.evict(id);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void setStatus(UUID id, OrderStatus status) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new com.example.orders.exception.OrderNotFoundException("Order not found: " + id));

        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());

        orderRepository.save(entity);
        orderCacheService.evict(id);
    }

    private OrderResponse createNewOrder(CreateOrderRequest request) {
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
        return orderMapper.toResponse(saved);
    }

    private OrderResponse waitForExistingOrder(String idempotencyKey) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(2).toMillis();

        while (System.currentTimeMillis() < deadline) {
            var currentValue = idempotencyService.getValue(idempotencyKey);

            if (currentValue.isPresent() && !idempotencyService.isProcessingValue(currentValue.get())) {
                return getOrder(UUID.fromString(currentValue.get()));
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted", e);
            }
        }

        throw new IdempotencyProcessingException("Request with the same Idempotency-Key is already being processed");
    }
}