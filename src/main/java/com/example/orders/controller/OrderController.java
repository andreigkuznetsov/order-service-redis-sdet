package com.example.orders.controller;

import com.example.orders.dto.*;
import com.example.orders.service.OrderProcessingService;
import com.example.orders.service.OrderService;
import com.example.orders.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RateLimitService rateLimitService;
    private final OrderProcessingService orderProcessingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                     @RequestHeader(value = "X-Client-Id", defaultValue = "anonymous") String clientId,
                                     @Valid @RequestBody CreateOrderRequest request) {
        rateLimitService.checkLimit(clientId);
        return orderService.createOrder(request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request);
    }

    @PostMapping("/{id}/process")
    public ProcessOrderResponse process(@PathVariable UUID id) {
        return orderProcessingService.process(id);
    }
}
