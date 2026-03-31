package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.factory.OrderRequestFactory;
import com.example.orders.support.RedisKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class OrderProcessingLockIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldProcessOrderOnlyOnceWhenRequestsAreConcurrent() throws Exception {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "lock-test");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> firstCall = () -> {
            readyLatch.countDown();
            startLatch.await();
            return processOrder(orderId);
        };

        Callable<Integer> secondCall = () -> {
            readyLatch.countDown();
            startLatch.await();
            return processOrder(orderId);
        };

        Future<Integer> firstFuture = executorService.submit(firstCall);
        Future<Integer> secondFuture = executorService.submit(secondCall);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        Integer firstStatus = firstFuture.get(10, TimeUnit.SECONDS);
        Integer secondStatus = secondFuture.get(10, TimeUnit.SECONDS);

        executorService.shutdown();

        assertThat(List.of(firstStatus, secondStatus))
                .containsExactlyInAnyOrder(200, 409);

        var savedOrder = orderRepository.findById(UUID.fromString(orderId)).orElseThrow();
        assertThat(savedOrder.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldReleaseLockAfterProcessingFinished() {
        String orderId = createOrder(OrderRequestFactory.validCreateOrderRequest(), "lock-test");
        String lockKey = RedisKeys.orderLock(orderId);

        int statusCode = processOrder(orderId);

        assertThat(statusCode).isEqualTo(200);
        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();
    }
}