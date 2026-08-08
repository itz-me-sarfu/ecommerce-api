package com.ecommerce.product;

import com.ecommerce.category.CategoryService;
import com.ecommerce.category.model.Category;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.StockUpdateRequest;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String query, String category, String brand,
                                                BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> specification = Specification.where(activeProducts())
                .and(textContains(query))
                .and(categoryMatches(category))
                .and(brandMatches(brand))
                .and(priceAtLeast(minPrice))
                .and(priceAtMost(maxPrice));
        Page<ProductResponse> page = productRepository.findAll(specification, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getActiveEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.findBySku(request.sku().trim()).isPresent()) {
            throw new ConflictException("A product with SKU '" + request.sku() + "' already exists.");
        }
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        if (!product.getSku().equalsIgnoreCase(request.sku().trim())
                && productRepository.findBySku(request.sku().trim()).isPresent()) {
            throw new ConflictException("A product with SKU '" + request.sku() + "' already exists.");
        }
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = getEntity(id);
        product.setStock(request.stock());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = getEntity(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public Product getActiveEntity(Long id) {
        Product product = getEntity(id);
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product with ID " + id + " was not found.");
        }
        return product;
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryService.getEntity(request.categoryId());
        product.setName(request.name().trim());
        product.setSku(request.sku().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setActive(true);
    }

    private Specification<Product> activeProducts() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private Specification<Product> textContains(String query) {
        return (root, criteriaQuery, cb) -> query == null || query.isBlank()
                ? null : cb.or(
                cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("description")), "%" + query.toLowerCase() + "%"));
    }

    private Specification<Product> categoryMatches(String category) {
        return (root, criteriaQuery, cb) -> {
            if (category == null || category.isBlank()) return null;
            var join = root.join("category", JoinType.INNER);
            String normalized = category.toLowerCase();
            return cb.or(cb.equal(cb.lower(join.get("slug")), normalized),
                    cb.equal(cb.lower(join.get("name")), normalized));
        };
    }

    private Specification<Product> brandMatches(String brand) {
        return (root, criteriaQuery, cb) -> brand == null || brand.isBlank()
                ? null : cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }

    private Specification<Product> priceAtLeast(BigDecimal price) {
        return (root, criteriaQuery, cb) -> price == null ? null : cb.greaterThanOrEqualTo(root.get("price"), price);
    }

    private Specification<Product> priceAtMost(BigDecimal price) {
        return (root, criteriaQuery, cb) -> price == null ? null : cb.lessThanOrEqualTo(root.get("price"), price);
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getDescription(),
                product.getBrand(), product.getPrice(), product.getStock(), product.getImageUrl(), product.isActive(),
                new ProductResponse.CategorySummary(category.getId(), category.getName(), category.getSlug()));
    }
}
