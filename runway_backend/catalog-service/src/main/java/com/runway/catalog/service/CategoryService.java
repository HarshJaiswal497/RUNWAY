package com.runway.catalog.service;

import com.runway.catalog.dto.request.CategoryRequest;
import com.runway.catalog.dto.response.CategoryResponse;
import com.runway.catalog.model.Category;
import com.runway.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        // Generate a simple slug: "Home Appliances" -> "home-appliances"
        category.setSlug(request.name().toLowerCase().replaceAll(" ", "-"));

        // Handle Parent Category if provided
        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getName(), saved.getSlug(), saved.getDescription(), saved.getParent() != null ? saved.getParent().getId() : 0);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllRootCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getParent() != null ? c.getParent().getId() : 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(
                        c.getId(),
                        c.getName(),
                        c.getSlug(),
                        c.getDescription(),
                        c.getParent() != null ? c.getParent().getId() : 0
                ))
                .toList();
    }

    public void deleteCategory(long id){
        categoryRepository.deleteById(id);
    }
}