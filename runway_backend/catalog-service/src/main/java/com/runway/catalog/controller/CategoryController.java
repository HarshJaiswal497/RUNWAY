package com.runway.catalog.controller;

import com.runway.catalog.dto.request.CategoryRequest;
import com.runway.catalog.dto.response.CategoryResponse;
import com.runway.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Only Admins manage the category tree
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getAllRootCategories());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Only Admins can delete categories
    public ResponseEntity<Void> deleteCategory(@PathVariable long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }



}