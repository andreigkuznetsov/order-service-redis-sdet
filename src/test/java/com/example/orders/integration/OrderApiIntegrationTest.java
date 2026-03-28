package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.entity.OrderStatus;
import com.example.orders.factory.OrderRequestFactory;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class OrderApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateOrder() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "test-client")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        assertThat(orderId).isNotBlank();
        assertThat(orderRepository.findById(UUID.fromString(orderId))).isPresent();
    }

    @Test
    void shouldGetOrderById() {
        CreateOrderRequest request = OrderRequestFactory.validCreateOrderRequest();

        String orderId =
                given()
                        .contentType(ContentType.JSON)
                        .header("X-Client-Id", "test-client")
                        .body(request)
                        .when()
                        .post("/api/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("id");

        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200)
                .body("id", org.hamcrest.Matchers.equalTo(orderId))
                .body("status", org.hamcrest.Matchers.equalTo(OrderStatus.NEW.name()));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        given()
                .when()
                .get("/api/orders/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
