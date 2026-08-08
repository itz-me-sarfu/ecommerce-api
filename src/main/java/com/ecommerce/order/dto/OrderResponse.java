package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String orderNumber, OrderStatus status, BigDecimal subtotal,
                            BigDecimal shippingFee, BigDecimal totalAmount, Instant createdAt,
                            ShippingAddress shippingAddress, List<Item> items) {
    public record ShippingAddress(String recipientName, String line1, String line2, String city,
                                   String state, String postalCode, String country) {}
    public record Item(Long productId, String productName, String sku, BigDecimal unitPrice, Integer quantity,
                       BigDecimal lineTotal) {}
}
