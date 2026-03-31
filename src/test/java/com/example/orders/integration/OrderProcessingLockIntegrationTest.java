package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class OrderProcessingLockIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldProcessOrderOnlyOnceWhenRequestsAreConcurrent() throws Exception {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "lock-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> firstCall = () -> {
            readyLatch.countDown();
            startLatch.await();

            return given()
                    .when()
                    .post("/api/orders/{id}/process", orderId)
                    .then()
                    .extract()
                    .statusCode();
        };

        Callable<Integer> secondCall = () -> {
            readyLatch.countDown();
            startLatch.await();

            return given()
                    .when()
                    .post("/api/orders/{id}/process", orderId)
                    .then()
                    .extract()
                    .statusCode();
        };

        Future<Integer> firstFuture = executorService.submit(firstCall);
        Future<Integer> secondFuture = executorService.submit(secondCall);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        Integer firstStatus = firstFuture.get(10, TimeUnit.SECONDS);
        Integer secondStatus = secondFuture.get(10, TimeUnit.SECONDS);

        executorService.shutdown();

        List<Integer> statuses = List.of(firstStatus, secondStatus);

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);

        UUID uuid = UUID.fromString(orderId);
        var savedOrder = orderRepository.findById(uuid).orElseThrow();

        assertThat(savedOrder.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldReleaseLockAfterProcessingFinished() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "lock-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String lockKey = "order:lock:" + orderId;

        given()
                .when()
                .post("/api/orders/{id}/process", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();
    }
}