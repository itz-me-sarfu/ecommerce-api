package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long id, List<Item> items, BigDecimal subtotal, int totalItems) {
    public record Item(Long productId, String name, String sku, BigDecimal unitPrice, Integer quantity,
                       BigDecimal lineTotal, String imageUrl) {}
}
