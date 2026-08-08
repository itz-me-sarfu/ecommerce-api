package com.ecommerce.cart;

import com.ecommerce.cart.dto.CartItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.ProductService;
import com.ecommerce.user.CurrentUserService;
import com.ecommerce.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CurrentUserService currentUserService;
    private final ProductService productService;

    @Transactional
    public CartResponse getCart(String email) {
        return toResponse(getOrCreate(email));
    }

    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {
        Cart cart = getOrCreate(email);
        Product product = productService.getActiveEntity(request.productId());
        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);
        int newQuantity = request.quantity() + (item == null ? 0 : item.getQuantity());
        validateStock(product, newQuantity);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            cart.getItems().add(item);
        }
        item.setQuantity(newQuantity);
        item.setUnitPrice(product.getPrice());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(String email, Long productId, CartItemRequest request) {
        Cart cart = getOrCreate(email);
        CartItem item = findItem(cart, productId);
        Product product = productService.getActiveEntity(productId);
        validateStock(product, request.quantity());
        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getPrice());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void removeItem(String email, Long productId) {
        Cart cart = getOrCreate(email);
        CartItem item = findItem(cart, productId);
        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    @Transactional
    public void clear(String email) {
        Cart cart = getOrCreate(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    public Cart getOrCreate(String email) {
        User user = currentUserService.get(email);
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setItems(new ArrayList<>());
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Product is not present in the cart."));
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
    }

    private CartResponse toResponse(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        var items = cart.getItems().stream().map(item -> {
            Product product = item.getProduct();
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartResponse.Item(product.getId(), product.getName(), product.getSku(), item.getUnitPrice(),
                    item.getQuantity(), lineTotal, product.getImageUrl());
        }).toList();
        return new CartResponse(cart.getId(), items, subtotal, totalItems);
    }
}
