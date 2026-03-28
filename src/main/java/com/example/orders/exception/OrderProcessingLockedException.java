package com.example.orders.exception;

public class OrderProcessingLockedException extends RuntimeException {
    public OrderProcessingLockedException(String message) {
        super(message);
    }
}
