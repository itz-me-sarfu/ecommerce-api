package com.ecommerce.admin;

import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                userRepository.count(),
                userRepository.countByRole(Role.CUSTOMER),
                userRepository.countByRole(Role.SELLER),
                productRepository.count(),
                productRepository.countByStockLessThanEqual(5),
                categoryRepository.count(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PLACED),
                orderRepository.countByStatus(OrderStatus.DELIVERED));
    }

    public record DashboardResponse(long totalUsers, long customers, long sellers, long totalProducts,
                                    long lowStockProducts, long totalCategories, long totalOrders,
                                    long placedOrders, long deliveredOrders) {}
}
