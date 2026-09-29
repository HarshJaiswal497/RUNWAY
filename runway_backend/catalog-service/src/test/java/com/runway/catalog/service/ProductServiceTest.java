package com.runway.catalog.service;

import com.runway.catalog.dto.request.ProductRequest;
import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.mapper.ProductMapper;
import com.runway.catalog.model.Category;
import com.runway.catalog.model.Media;
import com.runway.catalog.model.Product;
import com.runway.catalog.repository.CategoryRepository;
import com.runway.catalog.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private InventoryService inventoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Should successfully create a product when request is valid")
    void createProduct_Success() {
        // 1. Arrange
        Long sellerId = 100L;
        ProductRequest request = new ProductRequest(
                "SKU123", "Laptop", "Gaming Laptop",
                new BigDecimal("50000"), "INR", 1L, 10, null
        );

        Category category = new Category();
        category.setId(1L);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setTitle("Laptop");

        // Define Mock Behavior
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponse(any(Product.class))).thenReturn(new ProductResponse(
                1L, "SKU123", "Laptop", "Gaming Laptop",
                new BigDecimal("50000"), "INR", 1L, 100L, null, null
        ));

        // 2. Act
        ProductResponse response = productService.createProduct(request, null, sellerId);

        // 3. Assert
        assertNotNull(response);
        assertEquals("Laptop", response.title());
        verify(inventoryService, times(1)).createInitialInventory(any(), eq(10));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating a product owned by someone else")
    void updateProduct_Unauthorized() {
        // 1. Arrange
        Long productId = 1L;
        Long hackerId = 999L;
        Product existingProduct = new Product();
        existingProduct.setSellerId(100L); // Owned by User 100

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        ProductRequest updateRequest = new ProductRequest(
                "SKU123", "New Title", "Desc",
                BigDecimal.TEN, "INR", 1L, 0, null
        );

        // 2. Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.updateProduct(productId, updateRequest, hackerId, "CUSTOMER");
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cleanup files and delete product when user is owner")
    void deleteProduct_Success() {
        // 1. Arrange
        Long productId = 1L;
        Long ownerId = 100L;

        Product product = new Product();
        product.setSellerId(ownerId);

        // Create a fake media item
        Media media = new Media();
        media.setUrlPath("uploads/seller_100/image.jpg");
        product.getMedia().add(media);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // 2. Act
        productService.deleteProduct(productId, ownerId, "SELLER");

        // 3. Assert
        // Verify fileStorageService was called for the specific path
        verify(fileStorageService, times(1)).deleteFile("uploads/seller_100/image.jpg");
        // Verify repository delete was called
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to delete")
    void deleteProduct_Unauthorized() {
        // 1. Arrange
        Long productId = 1L;
        Long realOwnerId = 100L;
        Long intruderId = 999L;

        Product product = new Product();
        product.setSellerId(realOwnerId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () -> {
            productService.deleteProduct(productId, intruderId, "CUSTOMER");
        });

        // Verify delete was NEVER called
        verify(productRepository, never()).delete(any());
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("Should throw RuntimeException when product not found")
    void getProductById_NotFound() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.getProductById(1L);
        });

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    @DisplayName("getProductsByCategoryId - Should only return active products")
    void getProductsByCategoryId_FiltersActive() {
        // Arrange
        Long catId = 1L;
        Product activeProduct = new Product();
        activeProduct.setActive(true);

        Product inactiveProduct = new Product();
        inactiveProduct.setActive(false);

        // Mock repo to return only the active one (as the method name implies)
        when(productRepository.findByCategoryIdAndIsActiveTrue(catId))
                .thenReturn(List.of(activeProduct));

        when(productMapper.toResponse(any(Product.class)))
                .thenReturn(new ProductResponse(1L, "SKU1", "Active Item", "Desc", BigDecimal.TEN, "INR", catId, 10L, null, List.of()));

        // Act
        List<ProductResponse> results = productService.getProductsByCategoryId(catId);

        // Assert
        assertEquals(1, results.size());
        verify(productRepository).findByCategoryIdAndIsActiveTrue(catId);
    }

    @Test
    @DisplayName("getAllProducts - Should return list of all products")
    void getAllProducts_ReturnsList() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(new Product(), new Product()));

        // Act
        List<ProductResponse> results = productService.getAllProducts();

        // Assert
        assertEquals(2, results.size());
        verify(productRepository).findAll();
    }
}