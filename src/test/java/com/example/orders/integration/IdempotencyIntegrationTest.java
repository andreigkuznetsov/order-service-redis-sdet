package com.example.orders.integration;

import com.example.orders.base.BaseIntegrationTest;
import com.example.orders.factory.OrderRequestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateOnlyOneOrderForSameIdempotencyKey() {
        String idempotencyKey = "idem-123";

        String firstOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test",
                idempotencyKey
        );

        String secondOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test",
                idempotencyKey
        );

        assertThat(secondOrderId).isEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldCreateDifferentOrdersForDifferentIdempotencyKeys() {
        String firstOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test",
                "idem-1"
        );

        String secondOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test",
                "idem-2"
        );

        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldCreateDifferentOrdersWhenIdempotencyHeaderAbsent() {
        String firstOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test"
        );

        String secondOrderId = createOrder(
                OrderRequestFactory.validCreateOrderRequest(),
                "idem-test"
        );

        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
        assertThat(orderRepository.count()).isEqualTo(2);
    }
}