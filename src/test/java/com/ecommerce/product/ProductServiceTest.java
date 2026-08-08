package com.ecommerce.product;

import com.ecommerce.category.CategoryService;
import com.ecommerce.category.model.Category;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductMapsRequestAndCategory() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Electronics");
        category.setSlug("electronics");
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.empty());
        when(categoryService.getEntity(7L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(42L);
            return product;
        });

        ProductResponse response = productService.create(new ProductRequest(
                "Phone", "SKU-1", "A phone", "Acme", new BigDecimal("499.99"), 10, null, 7L));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Phone");
        assertThat(response.stock()).isEqualTo(10);
        assertThat(response.category().slug()).isEqualTo("electronics");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findByIdThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(101L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("101");
        verify(productRepository, never()).save(any());
    }
}
