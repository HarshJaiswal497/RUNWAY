package com.runway.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.runway.catalog.model.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Find all top-level categories
    List<Category> findByParentIsNull();

    // Find a category by its SEO-friendly slug
    Optional<Category> findBySlug(String slug);

    // Check if a name already exists in a specific parent category
    boolean existsByNameAndParentId(String name, Long parentId);
}
