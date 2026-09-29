package com.runway.catalog.service;

import com.runway.catalog.dto.request.ProductRequest;
import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.mapper.ProductMapper;
import com.runway.catalog.model.Category;
import com.runway.catalog.model.Media;
import com.runway.catalog.model.Product;
import com.runway.catalog.repository.CategoryRepository;
import com.runway.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final InventoryService inventoryService; // Injected our new service

    @Transactional
    public ProductResponse createProduct(ProductRequest request, List<MultipartFile> images, Long sellerId) {

        // 1. Validate Category
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.categoryId()));

        // 2. INBOUND MAPPING: Request -> Entity
        Product product = new Product();
        product.setSku(request.sku());
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "INR");
        product.setAttributes(request.attributes());
        product.setSellerId(sellerId);
        product.setCategory(category);

        // 3. Handle Media Uploads
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                try {
                    String relativePath = fileStorageService.saveFile(images.get(i), sellerId);
                    Media media = new Media();
                    media.setProduct(product);
                    media.setUrlPath(relativePath);
                    media.setPrimary(i == 0);
                    media.setSortOrder(i);
                    product.getMedia().add(media);
                } catch (IOException e) {
                    throw new RuntimeException("Could not store image: " + e.getMessage());
                }
            }
        }

        // 4. Save Product first (so we have an ID for the OneToOne relationship)
        Product savedProduct = productRepository.save(product);

        // 5. Initialize Inventory via InventoryService
        Integer initialQty = request.initialQuantity() != null ? request.initialQuantity() : 0;
        inventoryService.createInitialInventory(savedProduct, initialQty);

        // 6. OUTBOUND MAPPING
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return productMapper.toResponse(product);
    }
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId);
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }


    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long currentUserId, String role) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 1. OWNERSHIP CHECK
        if (!role.equalsIgnoreCase("ADMIN") && !product.getSellerId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: You do not own this product!");
        }

        // 2. Validate new Category if it's being changed
        if (!product.getCategory().getId().equals(request.categoryId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(newCategory);
        }

        // 3. Update Fields
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setCurrency(request.currency());
        product.setAttributes(request.attributes());

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long productId, Long currentUserId, String role) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!role.equalsIgnoreCase("ADMIN") && !product.getSellerId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: You cannot delete someone else's product!");
        }

        for (Media media : product.getMedia()) {
            fileStorageService.deleteFile(media.getUrlPath());
        }

        productRepository.delete(product);
    }
}