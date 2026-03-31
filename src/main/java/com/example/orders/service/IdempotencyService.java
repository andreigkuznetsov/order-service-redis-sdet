package com.example.orders.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.idempotency.ttl-hours}")
    private long ttlHours;

    public Optional<String> getValue(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(key)));
    }

    public boolean tryAcquireProcessing(String key) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                redisKey(key),
                PROCESSING,
                Duration.ofHours(ttlHours)
        );
        return Boolean.TRUE.equals(success);
    }

    public boolean isProcessingValue(String value) {
        return PROCESSING.equals(value);
    }

    public void saveOrderId(String key, String orderId) {
        redisTemplate.opsForValue().set(
                redisKey(key),
                orderId,
                Duration.ofHours(ttlHours)
        );
    }

    public void remove(String key) {
        redisTemplate.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return "order:idempotency:" + key;
    }
}