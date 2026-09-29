package com.runway.catalog.service;

import com.runway.catalog.exception.ResourceNotFoundException;
import com.runway.catalog.model.Inventory;
import com.runway.catalog.model.Product;
import com.runway.catalog.repository.InventoryRepository;
import com.runway.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Integer getStockForProduct(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(Inventory::getQuantity)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory/Product not found for ID: " + productId));
    }

    @Transactional
    public void updateStock(Long productId, Integer quantityChange, Long currentUserId, String roleHeader) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for Product ID: " + productId));

        // Convert the header to uppercase and check for keywords
        String roles = (roleHeader != null) ? roleHeader.toUpperCase() : "";

        // Check if any of the roles in the list match ADMIN or SELLER
        boolean isAdmin = roles.contains("ADMIN");
        boolean isSeller = roles.contains("SELLER");
        boolean isOwner = inventory.getProduct().getSellerId().equals(currentUserId);

        if (!isAdmin && !(isSeller && isOwner)) {
            throw new RuntimeException("Unauthorized: Role [" + roleHeader + "] cannot update stock!");
        }

        // 3. Logic Check: Prevent Negative Stock
        int newTotal = inventory.getQuantity() + quantityChange;
        if (newTotal < 0) {
            throw new RuntimeException("Insufficient stock! Available: " + inventory.getQuantity());
        }

        // 4. Update
        inventory.setQuantity(newTotal);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void createInitialInventory(Product product, Integer initialQuantity) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(initialQuantity);
        inventory.setLocation("Default Warehouse"); // Optional default
        inventoryRepository.save(inventory);
    }
}