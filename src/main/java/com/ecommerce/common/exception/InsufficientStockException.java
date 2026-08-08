package com.ecommerce.common.exception;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String productName, int requested, int available) {
        super("Insufficient stock for '" + productName + "'. Requested: " + requested + ", available: " + available);
    }
}
