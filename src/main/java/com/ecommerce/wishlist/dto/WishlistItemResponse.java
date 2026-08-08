package com.ecommerce.wishlist.dto;

import java.math.BigDecimal;

public record WishlistItemResponse(Long productId, String name, String sku, BigDecimal price,
                                   Integer stock, String imageUrl) {}
