package com.ecommerce.wishlist;

import com.ecommerce.product.ProductService;
import com.ecommerce.product.model.Product;
import com.ecommerce.user.CurrentUserService;
import com.ecommerce.user.model.User;
import com.ecommerce.wishlist.dto.WishlistItemResponse;
import com.ecommerce.wishlist.model.WishlistItem;
import com.ecommerce.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final CurrentUserService currentUserService;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> findAll(String email) {
        User user = currentUserService.get(email);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WishlistItemResponse add(String email, Long productId) {
        User user = currentUserService.get(email);
        Product product = productService.getActiveEntity(productId);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(user.getId(), productId).orElseGet(() -> {
            WishlistItem created = new WishlistItem();
            created.setUser(user);
            created.setProduct(product);
            return created;
        });
        return toResponse(wishlistRepository.save(item));
    }

    @Transactional
    public void remove(String email, Long productId) {
        User user = currentUserService.get(email);
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();
        return new WishlistItemResponse(product.getId(), product.getName(), product.getSku(), product.getPrice(),
                product.getStock(), product.getImageUrl());
    }
}
