package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateOnlyOneOrderForSameIdempotencyKey() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String idempotencyKey = "idem-123";

        String firstOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .header("Idempotency-Key", idempotencyKey)
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String secondOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .header("Idempotency-Key", idempotencyKey)
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        assertThat(secondOrderId).isEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldCreateDifferentOrdersForDifferentIdempotencyKeys() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String firstOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .header("Idempotency-Key", "idem-1")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String secondOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .header("Idempotency-Key", "idem-2")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldCreateDifferentOrdersWhenIdempotencyHeaderAbsent() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String firstOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        String secondOrderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "idem-test")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(2);
    }
}
