package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldAllowRequestsWithinLimit() {
        String clientId = "rate-limit-ok";

        for (int i = 0; i < 5; i++) {
            createOrder(OrderRequestFactory.validCreateOrderRequest(), clientId);
        }
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() {
        String clientId = "rate-limit-exceeded";

        for (int i = 0; i < 5; i++) {
            createOrder(OrderRequestFactory.validCreateOrderRequest(), clientId);
        }

        given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(OrderRequestFactory.validCreateOrderRequest())
                .when()
                .post("/api/orders")
                .then()
                .statusCode(429);
    }

    @Test
    void shouldAllowRequestsAgainAfterRateLimitWindowExpires() {
        String clientId = "rate-limit-reset";

        for (int i = 0; i < 5; i++) {
            createOrder(OrderRequestFactory.validCreateOrderRequest(), clientId);
        }

        given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(OrderRequestFactory.validCreateOrderRequest())
                .when()
                .post("/api/orders")
                .then()
                .statusCode(429);

        await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() ->
                        given()
                                .contentType(ContentType.JSON)
                                .header("X-Client-Id", clientId)
                                .body(OrderRequestFactory.validCreateOrderRequest())
                                .when()
                                .post("/api/orders")
                                .then()
                                .statusCode(201)
                );
    }
}