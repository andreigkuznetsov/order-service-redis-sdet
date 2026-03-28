package com.example.orders.service;

import com.example.orders.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.order-ttl-seconds}")
    private long orderTtlSeconds;

    public Optional<OrderResponse> get(UUID orderId) {
        String value = redisTemplate.opsForValue().get(cacheKey(orderId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(readValue(value));
    }

    public void put(OrderResponse response) {
        writeValue(response.id(), response);
    }

    public void evict(UUID orderId) {
        redisTemplate.delete(cacheKey(orderId));
    }

    public String cacheKey(UUID orderId) {
        return "order:cache:" + orderId;
    }

    @SneakyThrows(JsonProcessingException.class)
    private OrderResponse readValue(String value) {
        return objectMapper.readValue(value, OrderResponse.class);
    }

    @SneakyThrows(JsonProcessingException.class)
    private void writeValue(UUID orderId, OrderResponse response) {
        redisTemplate.opsForValue().set(
                cacheKey(orderId),
                objectMapper.writeValueAsString(response),
                Duration.ofSeconds(orderTtlSeconds)
        );
    }
}
