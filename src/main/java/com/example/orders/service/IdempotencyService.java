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

    private final StringRedisTemplate redisTemplate;

    @Value("${app.idempotency.ttl-hours}")
    private long ttlHours;

    public Optional<String> getSavedOrderId(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(key)));
    }

    public void save(String key, String orderId) {
        redisTemplate.opsForValue().set(redisKey(key), orderId, Duration.ofHours(ttlHours));
    }

    private String redisKey(String key) {
        return "order:idempotency:" + key;
    }
}
