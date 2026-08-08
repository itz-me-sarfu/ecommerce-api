package com.ecommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 4000) String description,
        @Size(max = 100) String brand,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @Size(max = 500) String imageUrl,
        @NotNull Long categoryId
) {}
