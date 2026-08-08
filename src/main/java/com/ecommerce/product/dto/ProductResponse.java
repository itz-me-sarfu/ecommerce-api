package com.ecommerce.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        String brand,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        boolean active,
        CategorySummary category
) {
    public record CategorySummary(Long id, String name, String slug) {}
}
