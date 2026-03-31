package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldAllowRequestsWithinLimit() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();
        String clientId = "rate-limit-ok";

        for (int i = 0; i < 5; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("X-Client-Id", clientId)
                    .body(request)
                    .when()
                    .post("/api/orders")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();
        String clientId = "rate-limit-exceeded";

        for (int i = 0; i < 5; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("X-Client-Id", clientId)
                    .body(request)
                    .when()
                    .post("/api/orders")
                    .then()
                    .statusCode(201);
        }

        given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(429);
    }

    @Test
    void shouldAllowRequestsAgainAfterRateLimitWindowExpires() throws InterruptedException {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();
        String clientId = "rate-limit-reset";

        for (int i = 0; i < 5; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("X-Client-Id", clientId)
                    .body(request)
                    .when()
                    .post("/api/orders")
                    .then()
                    .statusCode(201);
        }

        given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(429);

        Thread.sleep(4000);

        given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201);
    }
}