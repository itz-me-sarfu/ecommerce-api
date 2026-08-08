package com.ecommerce.config;

import com.ecommerce.category.model.Category;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Value("${SEED_ADMIN_PASSWORD:Admin@12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User admin = new User();
            admin.setFullName("Platform Admin");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        seedCategory("Electronics", "electronics", "Phones, laptops, and accessories");
        seedCategory("Fashion", "fashion", "Clothing and accessories");
        seedCategory("Home", "home", "Furniture and home essentials");
    }

    private void seedCategory(String name, String slug, String description) {
        if (categoryRepository.findBySlug(slug).isEmpty()) {
            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);
            category.setDescription(description);
            categoryRepository.save(category);
        }
    }
}
