package com.runway.order.service;

import com.runway.order.client.CatalogServiceClient;
import com.runway.order.dto.catalog.ProductDTO;
import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.exception.ValidationException;
import com.runway.order.model.Cart;
import com.runway.order.model.CartItem;
import com.runway.order.repository.CartItemRepository;
import com.runway.order.repository.CartRepository;
import com.runway.order.util.Dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CartServiceImplTest — Unit Tests
 *
 * Uses Mockito to mock repositories and CatalogServiceClient — no DB or network needed.
 * Tests all business rules in CartServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceImplTest {

    @Mock private CartRepository        cartRepository;
    @Mock private CartItemRepository    cartItemRepository;
    @Mock private CatalogServiceClient  catalogClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final Long USER_ID    = 1L;
    private static final Long PRODUCT_ID = 100L;

    private Cart emptyCart;
    private Cart cartWithItems;

    private ProductDTO sampleProduct;

    @BeforeEach
    void setUp() {
        emptyCart = Cart.builder()
            .id(1L)
            .userId(USER_ID)
            .items(new ArrayList<>())
            .build();

        CartItem item = CartItem.builder()
            .id(1L)
            .cart(emptyCart)
            .productId(PRODUCT_ID)
            .productTitle("Test Product")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(1000))
            .build();

        cartWithItems = Cart.builder()
            .id(1L)
            .userId(USER_ID)
            .items(new ArrayList<>(List.of(item)))
            .build();

        sampleProduct = new ProductDTO();
        sampleProduct.setId(PRODUCT_ID);
        sampleProduct.setTitle("Test Product");
        sampleProduct.setPrice(BigDecimal.valueOf(999));
        sampleProduct.setCurrency("INR");
    }

    // ── getMyCart Tests ───────────────────────────────────────

    @Test
    @DisplayName("getMyCart() — creates new cart if user has none")
    void testGetMyCartCreatesNewCart() {
        when(cartRepository.findByUserIdWithItems(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(emptyCart));

        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);
        CartResponse response = cartService.getMyCart(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getMyCart() — returns existing cart")
    void testGetMyCartReturnsExisting() {
        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(cartWithItems));

        CartResponse response = cartService.getMyCart(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    // ── addItem Tests ─────────────────────────────────────────

    @Test
    @DisplayName("addItem() — successfully adds new item to cart")
    void testAddItemSuccess() {
        AddCartItemRequest request = new AddCartItemRequest(
            PRODUCT_ID, "Test Product", BigDecimal.valueOf(999), 1);

        // Mock Feign calls
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(sampleProduct);
        when(catalogClient.getStock(PRODUCT_ID)).thenReturn(100);

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, PRODUCT_ID))
            .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));

        CartResponse response = cartService.addItem(USER_ID, request);

        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItem() — throws ValidationException for quantity 0")
    void testAddItemZeroQuantityThrows() {
        AddCartItemRequest request = new AddCartItemRequest(
            PRODUCT_ID, "Test Product", BigDecimal.valueOf(999), 0);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Quantity must be at least 1");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItem() — throws ValidationException when catalog service returns no product")
    void testAddItemCatalogFailureThrows() {
        AddCartItemRequest request = new AddCartItemRequest(
            PRODUCT_ID, "Test Product", BigDecimal.ZERO, 1);

        // Feign call throws → service catches and wraps as "Product not found in catalog"
        when(catalogClient.getProductById(PRODUCT_ID))
            .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Product not found in catalog");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItem() — increases quantity if product already in cart")
    void testAddItemIncreasesExistingQuantity() {
        CartItem existingItem = CartItem.builder()
            .id(1L).cart(emptyCart).productId(PRODUCT_ID)
            .productTitle("Test Product").quantity(3)
            .unitPrice(BigDecimal.valueOf(999)).build();

        AddCartItemRequest request = new AddCartItemRequest(
            PRODUCT_ID, "Test Product", BigDecimal.valueOf(999), 2);

        // Mock Feign calls
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(sampleProduct);
        when(catalogClient.getStock(PRODUCT_ID)).thenReturn(100);

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, PRODUCT_ID))
            .thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));

        cartService.addItem(USER_ID, request);

        assertThat(existingItem.getQuantity()).isEqualTo(5); // 3 + 2
        verify(cartItemRepository).save(existingItem);
    }

    // ── removeItem Tests ──────────────────────────────────────

    @Test
    @DisplayName("removeItem() — successfully removes item from cart")
    void testRemoveItemSuccess() {
        when(cartRepository.findByUserId(USER_ID))
            .thenReturn(Optional.of(cartWithItems));
        when(cartItemRepository.existsByCartIdAndProductId(1L, PRODUCT_ID))
            .thenReturn(true);
        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));

        CartResponse response = cartService.removeItem(USER_ID, PRODUCT_ID);

        verify(cartItemRepository)
            .deleteByCartIdAndProductId(1L, PRODUCT_ID);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("removeItem() — throws ResourceNotFoundException if item not in cart")
    void testRemoveItemNotFoundThrows() {
        when(cartRepository.findByUserId(USER_ID))
            .thenReturn(Optional.of(emptyCart));
        when(cartItemRepository.existsByCartIdAndProductId(1L, PRODUCT_ID))
            .thenReturn(false);

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, PRODUCT_ID))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(cartItemRepository, never()).deleteByCartIdAndProductId(any(), any());
    }

    // ── clearCart Tests ───────────────────────────────────────

    @Test
    @DisplayName("clearCart() — removes all items from cart")
    void testClearCartSuccess() {
        when(cartRepository.findByUserId(USER_ID))
            .thenReturn(Optional.of(cartWithItems));
        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));

        CartResponse response = cartService.clearCart(USER_ID);

        verify(cartItemRepository).deleteAllByCartId(1L);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("clearCart() — throws ResourceNotFoundException if no cart")
    void testClearCartNotFoundThrows() {
        when(cartRepository.findByUserId(USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.clearCart(USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
