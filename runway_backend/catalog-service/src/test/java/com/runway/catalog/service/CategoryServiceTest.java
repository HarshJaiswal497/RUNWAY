package com.runway.catalog.service;

import com.runway.catalog.dto.request.CategoryRequest;
import com.runway.catalog.dto.response.CategoryResponse;
import com.runway.catalog.model.Category;
import com.runway.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Create Category - Should generate valid slug and save")
    void createCategory_Success() {
        // Arrange
        CategoryRequest request = new CategoryRequest("Home Appliances", "Desc", null);

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        // Act
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertEquals("home-appliances", response.slug());
        assertEquals("Home Appliances", response.name());
        verify(categoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Create Sub-Category - Should link to parent")
    void createSubCategory_Success() {
        // Arrange
        Category parent = new Category();
        parent.setId(10L);
        parent.setName("Electronics");

        CategoryRequest request = new CategoryRequest("Laptops", "Description", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });

        // Act
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertNotNull(response);
        assertEquals("laptops", response.slug());
        verify(categoryRepository).findById(10L);
    }
}