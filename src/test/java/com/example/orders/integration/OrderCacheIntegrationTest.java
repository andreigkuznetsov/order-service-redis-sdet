package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class OrderCacheIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldSaveOrderToCacheAfterFirstGet() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "cache-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String redisKey = "order:cache:" + orderId;

        // cache должен быть пуст до первого GET
        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();

        // первый GET → прогревает cache
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);

        // теперь cache должен появиться
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
    }

    @Test
    void shouldReturnOrderFromCacheOnSecondGet() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "cache-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String redisKey = "order:cache:" + orderId;

        // первый GET → кладёт в cache
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        // второй GET → должен идти из cache (мы не можем напрямую проверить источник,
        // но можем проверить, что cache уже есть и не исчез)
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
    }

    @Test
    void shouldInvalidateCacheAfterStatusUpdate() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "cache-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String redisKey = "order:cache:" + orderId;

        // прогреваем cache
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        // обновляем статус → cache должен инвалидироваться
        given()
                .contentType(ContentType.JSON)
                .body("{\"status\":\"COMPLETED\"}")
                .when()
                .patch("/api/orders/{id}/status", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();
    }

    @Test
    void shouldExpireCacheAfterTtl() throws InterruptedException {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "cache-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String redisKey = "order:cache:" + orderId;

        // первый GET → кладём в cache
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();

        // ждём TTL (в тестах у тебя ~2 секунды)
        Thread.sleep(3000);

        // cache должен исчезнуть
        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();
    }
}
