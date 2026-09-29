package com.runway.engagement.reviews.controller;

import com.runway.engagement.reviews.dto.RatingMapper;
import com.runway.engagement.reviews.dto.RatingRequest;
import com.runway.engagement.reviews.dto.RatingResponse;
import com.runway.engagement.reviews.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(
        name = "Ratings",
        description = "Product ratings and rating analytics"
)
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    //  CUSTOMER / ADMIN: create a new rating for a product
    @PostMapping("/products/{productId}/ratings")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Create product rating",
            description = "Creates a new rating for a product by the logged-in user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rating created successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RatingResponse> createRating(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody RatingRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        RatingMapper.toResponse(
                                ratingService.createRating(
                                        productId,
                                        userId,
                                        request.getRating()
                                )
                        )
                );
    }

    //  CUSTOMER / ADMIN: update my existing rating for a product
    @PutMapping("/products/{productId}/ratings")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Update product rating",
            description = "Updates the rating for a product by the logged-in user"
    )
    public ResponseEntity<RatingResponse> updateRating(
            @PathVariable Long productId,
            @Valid @RequestBody RatingRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                RatingMapper.toResponse(
                        ratingService.updateRating(
                                productId,
                                userId,
                                request.getRating()
                        )
                )
        );
    }

    //  CUSTOMER / ADMIN: delete my rating for a product
    @DeleteMapping("/products/{productId}/ratings")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Delete product rating",
            description = "Deletes the logged-in user's rating for a product"
    )
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        ratingService.deleteRating(productId, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    //  PUBLIC: ratings for a product (supports mine=true)
    @GetMapping("/products/{productId}/ratings")
    @Operation(
            summary = "Get ratings for product",
            description = "Retrieves all ratings for a product or only the logged-in user's rating"
    )
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RatingResponse>> getRatingsForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "false") boolean mine,
            Authentication authentication
    ) {
        if (mine) {
            if (authentication == null) {
                throw new AccessDeniedException("Authentication required for mine=true");
            }

            Long userId = (Long) authentication.getPrincipal();
            return ResponseEntity.ok(
                    List.of(
                            RatingMapper.toResponse(
                                    ratingService.getUserRatingForProduct(productId, userId)
                            )
                    )
            );
        }

        return ResponseEntity.ok(
                ratingService.getRatingsByProductId(productId)
                        .stream()
                        .map(RatingMapper::toResponse)
                        .toList()
        );
    }

    //  PUBLIC: average rating for a product
    @GetMapping("/products/{productId}/ratings/average")
    @Operation(
            summary = "Get average product rating",
            description = "Retrieves the average rating for a product"
    )
    @PreAuthorize("permitAll()")
    public ResponseEntity<Double> getAverageRating(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                ratingService.getAverageRatingByProductId(productId)
        );
    }

    //  CUSTOMER / ADMIN: all ratings created by the logged-in user or all ratings for admin
    @GetMapping("/ratings")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Get ratings",
            description = "Retrieves ratings; supports mine=true for logged-in user or full list for admin"
    )
    public ResponseEntity<List<RatingResponse>> getRatings(
            @RequestParam(defaultValue = "false") boolean mine,
            Authentication authentication
    ) {
        if (mine) {
            Long userId = (Long) authentication.getPrincipal();
            return ResponseEntity.ok(
                    ratingService.getUserRatings(userId)
                            .stream()
                            .map(RatingMapper::toResponse)
                            .toList()
            );
        }

        return ResponseEntity.ok(
                ratingService.getAllRatings()
                        .stream()
                        .map(RatingMapper::toResponse)
                        .toList()
        );
    }
}