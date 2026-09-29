package com.runway.catalog.repository;

import com.runway.catalog.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    // Get the primary thumbnail for a product
    Optional<Media> findByProductIdAndIsPrimaryTrue(Long productId);

    // Get all media for a product ordered by the seller's preference
    List<Media> findByProductIdOrderBySortOrderAsc(Long productId);
}
