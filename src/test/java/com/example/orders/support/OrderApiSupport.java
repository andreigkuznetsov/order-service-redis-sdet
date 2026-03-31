package com.example.orders.support;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.UpdateOrderStatusRequest;
import com.example.orders.entity.OrderStatus;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public abstract class OrderApiSupport {

    protected String createOrder(CreateOrderRequest request, String clientId) {
        return given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");
    }

    protected String createOrder(CreateOrderRequest request, String clientId, String idempotencyKey) {
        return given()
                .contentType(ContentType.JSON)
                .header("X-Client-Id", clientId)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");
    }

    protected void getOrderOk(String orderId) {
        given()
                .when()
                .get("/api/orders/{id}", orderId)
                .then()
                .statusCode(200);
    }

    protected void updateOrderStatusOk(String orderId, OrderStatus status) {
        given()
                .contentType(ContentType.JSON)
                .body(new UpdateOrderStatusRequest(status))
                .when()
                .patch("/api/orders/{id}/status", orderId)
                .then()
                .statusCode(200);
    }

    protected int processOrder(String orderId) {
        return given()
                .when()
                .post("/api/orders/{id}/process", orderId)
                .then()
                .extract()
                .statusCode();
    }
}
