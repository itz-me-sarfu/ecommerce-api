package com.ecommerce.order;

import com.ecommerce.address.AddressService;
import com.ecommerce.address.model.Address;
import com.ecommerce.cart.CartService;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.CurrentUserService;
import com.ecommerce.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AddressService addressService;
    @Mock private CartService cartService;
    @Mock private ProductRepository productRepository;

    private OrderService orderService;
    private User user;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, currentUserService, addressService, cartService, productRepository);
        user = new User();
        user.setId(1L);
        user.setEmail("customer@example.com");

        product = new Product();
        product.setId(10L);
        product.setName("Phone");
        product.setSku("PHONE-1");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(5);
        product.setActive(true);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cart = new Cart();
        cart.getItems().add(cartItem);
    }

    @Test
    void placeOrderReservesStockAndClearsCart() {
        Address address = address();
        when(currentUserService.get("customer@example.com")).thenReturn(user);
        when(addressService.getOwnedEntity(20L, 1L)).thenReturn(address);
        when(cartService.getOrCreate("customer@example.com")).thenReturn(cart);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(30L);
            return order;
        });

        var response = orderService.placeOrder("customer@example.com", new PlaceOrderRequest(20L));

        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(product.getStock()).isEqualTo(3);
        assertThat(cart.getItems()).isEmpty();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void placeOrderRejectsInsufficientStockWithoutChangingInventory() {
        product.setStock(1);
        when(currentUserService.get("customer@example.com")).thenReturn(user);
        when(addressService.getOwnedEntity(20L, 1L)).thenReturn(address());
        when(cartService.getOrCreate("customer@example.com")).thenReturn(cart);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder("customer@example.com", new PlaceOrderRequest(20L)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStock()).isEqualTo(1);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void invalidStateTransitionIsRejected() {
        Order order = new Order();
        order.setId(30L);
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(30L, OrderStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PLACED -> DELIVERED");
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Address address() {
        Address address = new Address();
        address.setId(20L);
        address.setRecipientName("Customer");
        address.setLine1("1 Main Street");
        address.setCity("Delhi");
        address.setState("Delhi");
        address.setPostalCode("110001");
        address.setCountry("India");
        return address;
    }
}
