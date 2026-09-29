package com.runway.catalog.service;

import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.mapper.ProductMapper;
import com.runway.catalog.model.Product;
import com.runway.catalog.model.WishlistItem;
import com.runway.catalog.repository.ProductRepository;
import com.runway.catalog.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository  productRepository;
    private final ProductMapper      productMapper;

    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        // Prevent duplicate — just return if already wishlisted
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .product(product)
                .build();

        wishlistRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .stream()
                .map(item -> productMapper.toResponse(item.getProduct()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
}