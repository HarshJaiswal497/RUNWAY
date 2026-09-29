package com.runway.catalog.controller;

import com.runway.catalog.service.FileStorageService;
import com.runway.catalog.model.Media;
import com.runway.catalog.model.Product;
import com.runway.catalog.repository.MediaRepository;
import com.runway.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/catalog/products")
@RequiredArgsConstructor
public class MediaController {

    private final ProductRepository  productRepository;
    private final MediaRepository    mediaRepository;
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> addImages(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Ownership check
        if (!role.equalsIgnoreCase("ADMIN") && !product.getSellerId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this product!");
        }

        // Force initialize the media collection inside the transaction
        int currentCount = product.getMedia().size();

        for (int i = 0; i < images.size(); i++) {
            try {
                String relativePath = fileStorageService.saveFile(images.get(i), userId);
                Media media = new Media();
                media.setProduct(product);
                media.setUrlPath(relativePath);
                media.setPrimary(currentCount == 0 && i == 0);
                media.setSortOrder(currentCount + i);
                product.getMedia().add(media);
            } catch (IOException e) {
                throw new RuntimeException("Could not store image: " + e.getMessage());
            }
        }

        productRepository.save(product);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/images/{mediaId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long mediaId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    ) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));

        Product product = media.getProduct();
        if (!role.equalsIgnoreCase("ADMIN") && !product.getSellerId().equals(userId)) {
            throw new RuntimeException("Unauthorized!");
        }

        boolean wasPrimary = media.isPrimary();
        Long deletedMediaId = media.getId();

        // Delete file from storage
        fileStorageService.deleteFile(media.getUrlPath());
        mediaRepository.delete(media);
        mediaRepository.flush(); // ← ensure delete is committed before querying

        // If deleted image was primary, make first remaining image primary
        if (wasPrimary) {
            product.getMedia().stream()
                    .filter(m -> !m.getId().equals(deletedMediaId))
                    .findFirst()
                    .ifPresent(m -> {
                        m.setPrimary(true);
                        mediaRepository.save(m);
                    });
        }

        return ResponseEntity.noContent().build();
    }
}