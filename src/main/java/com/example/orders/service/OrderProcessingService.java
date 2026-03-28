package com.example.orders.service;

import com.example.orders.dto.ProcessOrderResponse;
import com.example.orders.entity.OrderStatus;
import com.example.orders.exception.OrderProcessingLockedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final StringRedisTemplate redisTemplate;
    private final OrderService orderService;

    @Value("${app.lock.ttl-seconds}")
    private long lockTtlSeconds;

    public ProcessOrderResponse process(UUID orderId) {
        String lockKey = "order:lock:" + orderId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(lockTtlSeconds));

        if (!Boolean.TRUE.equals(locked)) {
            throw new OrderProcessingLockedException("Order is already being processed");
        }

        try {
            orderService.setStatus(orderId, OrderStatus.PROCESSING);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted", e);
            }
            orderService.setStatus(orderId, OrderStatus.COMPLETED);

            return new ProcessOrderResponse(
                    "Order processed",
                    orderId
            );
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
