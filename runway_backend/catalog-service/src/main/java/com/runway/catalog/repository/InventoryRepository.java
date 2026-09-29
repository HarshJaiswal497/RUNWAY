package com.runway.catalog.repository;

import com.runway.catalog.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Find inventory for a specific product
    Optional<Inventory> findByProductId(Long productId);

    // Find all products that are low on stock (e.g., less than 5)
    List<Inventory> findByQuantityLessThan(Integer threshold);
}
