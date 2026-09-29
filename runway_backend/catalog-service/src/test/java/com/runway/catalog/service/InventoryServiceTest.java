package com.runway.catalog.service;

import com.runway.catalog.model.Inventory;
import com.runway.catalog.model.Product;
import com.runway.catalog.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setSellerId(100L); // Owner is 100

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(50);
    }

    @Test
    @DisplayName("Update Stock - Success as Owner")
    void updateStock_Success() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        inventoryService.updateStock(1L, 10, 100L, "SELLER");

        assertEquals(60, inventory.getQuantity());
        verify(inventoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Update Stock - Fail as Intruder")
    void updateStock_Unauthorized() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        Exception exception = assertThrows(RuntimeException.class, () ->
                inventoryService.updateStock(1L, 10, 999L, "SELLER")
        );

        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("Update Stock - Fail if Result is Negative")
    void updateStock_NegativeResult() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        Exception exception = assertThrows(RuntimeException.class, () ->
                inventoryService.updateStock(1L, -100, 100L, "SELLER")
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }
}