package com.example.orders.exception;

public class IdempotencyProcessingException extends RuntimeException {
    public IdempotencyProcessingException(String message) {
        super(message);
    }
}
