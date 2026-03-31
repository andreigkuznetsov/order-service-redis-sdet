package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.factory.OrderRequestFactory;
import com.example.orders.support.RedisKeys;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderCacheIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldSaveOrderToCacheAfterFirstGet() {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "cache-test");
        String redisKey = RedisKeys.orderCache(orderId);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();

        getOrderOk(orderId);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
    }

    @Test
    void shouldReturnOrderFromCacheOnSecondGet() {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "cache-test");
        String redisKey = RedisKeys.orderCache(orderId);

        getOrderOk(orderId);
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        getOrderOk(orderId);
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
    }

    @Test
    void shouldInvalidateCacheAfterStatusUpdate() {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "cache-test");
        String redisKey = RedisKeys.orderCache(orderId);

        getOrderOk(orderId);
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        updateOrderStatusOk(orderId, com.example.orders.entity.OrderStatus.COMPLETED);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();
    }

    @Test
    void shouldExpireCacheAfterTtl() {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "cache-test");
        String redisKey = RedisKeys.orderCache(orderId);

        getOrderOk(orderId);
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        await()
                .atMost(java.time.Duration.ofSeconds(4))
                .untilAsserted(() -> assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse());
    }
}