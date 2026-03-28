package com.example.orders.service;

import com.example.orders.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.max-requests}")
    private long maxRequests;

    @Value("${app.rate-limit.window-seconds}")
    private long windowSeconds;

    public void checkLimit(String clientId) {
        String key = "order:rate_limit:" + clientId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (count != null && count > maxRequests) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
    }
}
