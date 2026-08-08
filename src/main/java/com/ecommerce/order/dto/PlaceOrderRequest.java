package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(@NotNull Long addressId) {}
