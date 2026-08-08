package com.ecommerce.order;

import com.ecommerce.address.AddressService;
import com.ecommerce.address.model.Address;
import com.ecommerce.cart.CartService;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.CurrentUserService;
import com.ecommerce.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("5.99");

    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final AddressService addressService;
    private final CartService cartService;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = currentUserService.get(email);
        Address address = addressService.getOwnedEntity(request.addressId(), user.getId());
        Cart cart = cartService.getOrCreate(email);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart.");
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        copyAddress(order, address);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product in cart was not found."));
            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer available.");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException(product.getName(), cartItem.getQuantity(), product.getStock());
            }

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.saveAndFlush(product); // @Version prevents concurrent overselling.

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            order.getItems().add(item);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING;
        order.setSubtotal(money(subtotal));
        order.setShippingFee(shipping);
        order.setTotalAmount(money(subtotal.add(shipping)));
        cart.getItems().clear();
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(String email) {
        User user = currentUserService.get(email);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findMineById(String email, Long id) {
        User user = currentUserService.get(email);
        return toResponse(orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " was not found.")));
    }

    @Transactional
    public OrderResponse cancelMine(String email, Long id) {
        User user = currentUserService.get(email);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " was not found."));
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Only orders in PLACED state can be cancelled.");
        }
        restock(order);
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus nextStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " was not found."));
        if (!allowedTransitions().getOrDefault(order.getStatus(), List.of()).contains(nextStatus)) {
            throw new BadRequestException("Invalid order transition: " + order.getStatus() + " -> " + nextStatus);
        }
        if (nextStatus == OrderStatus.CANCELLED) {
            restock(order);
        }
        order.setStatus(nextStatus);
        return toResponse(orderRepository.save(order));
    }

    private void restock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            Product product = productRepository.findById(item.getProduct().getId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.saveAndFlush(product);
            }
        }
    }

    private Map<OrderStatus, List<OrderStatus>> allowedTransitions() {
        Map<OrderStatus, List<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PLACED, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.CONFIRMED, List.of(OrderStatus.PACKED));
        transitions.put(OrderStatus.PACKED, List.of(OrderStatus.SHIPPED));
        transitions.put(OrderStatus.SHIPPED, List.of(OrderStatus.DELIVERED));
        return transitions;
    }

    private void copyAddress(Order order, Address address) {
        order.setRecipientName(address.getRecipientName());
        order.setShippingLine1(address.getLine1());
        order.setShippingLine2(address.getLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private OrderResponse toResponse(Order order) {
        var items = order.getItems().stream().map(item -> new OrderResponse.Item(
                item.getProduct() == null ? null : item.getProduct().getId(), item.getProductName(), item.getSku(),
                item.getUnitPrice(), item.getQuantity(), money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))).toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getSubtotal(),
                order.getShippingFee(), order.getTotalAmount(), order.getCreatedAt(),
                new OrderResponse.ShippingAddress(order.getRecipientName(), order.getShippingLine1(), order.getShippingLine2(),
                        order.getShippingCity(), order.getShippingState(), order.getShippingPostalCode(), order.getShippingCountry()), items);
    }
}
