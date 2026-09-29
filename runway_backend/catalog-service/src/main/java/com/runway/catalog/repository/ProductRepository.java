package com.runway.catalog.repository;

import com.runway.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // For the Seller Dashboard: "Show me all my products"
    List<Product> findBySellerId(Long sellerId);

    // For the Storefront: "Show me all active products in this category"
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    //Search by SKU
    Optional<Product> findBySku(String sku);

    // Search inside the JSON 'attributes' column (Native MySQL Query)
    @Query(value = "SELECT * FROM products WHERE attributes->>?1 = ?2", nativeQuery = true)
    List<Product> findByAttribute(String key, String value);
}
