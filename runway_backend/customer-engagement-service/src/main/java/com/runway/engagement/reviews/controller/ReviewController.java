package com.runway.engagement.reviews.controller;

import com.runway.engagement.reviews.dto.ReviewMapper;
import com.runway.engagement.reviews.dto.ReviewRequest;
import com.runway.engagement.reviews.dto.ReviewResponse;
import com.runway.engagement.reviews.dto.ReviewStatusUpdateRequest;
import com.runway.engagement.reviews.model.Review;
import com.runway.engagement.reviews.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
        name = "Reviews",
        description = "Product reviews and moderation")

public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    //  CUSTOMER / ADMIN: create review for product (status = PENDING)
    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Create product review",
            description = "Creates a review for a product. Review is created in PENDING status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created"),
            @ApiResponse(responseCode = "403", description = "User has not purchased product")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        Review review = ReviewMapper.toEntity(request);
        review.setProductId(productId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ReviewMapper.toResponse(
                                reviewService.createReview(review, userId)
                        )
                );
    }

    //  PUBLIC: approved reviews for product
    //  CUSTOMER: mine=true -> my review(s) for this product
    @GetMapping("/products/{productId}/reviews")
    @Operation(
            summary = "Get product reviews",
            description = "Returns approved reviews for a product. " +
                    "If mine=true, returns only the logged-in user's reviews."
    )
    @ApiResponse(responseCode = "200", description = "Reviews retrieved")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "false") boolean mine,
            Authentication authentication
    ) {
        if (mine) {
            if (authentication == null) {
                throw new AccessDeniedException(
                        "Authentication required for mine=true"
                );
            }

            Long userId = (Long) authentication.getPrincipal();
            return ResponseEntity.ok(
                    reviewService.getReviewsByUserForProduct(productId, userId)
                            .stream()
                            .map(ReviewMapper::toResponse)
                            .toList()
            );
        }

        return ResponseEntity.ok(
                reviewService.getApprovedReviewsByProduct(productId)
                        .stream()
                        .map(ReviewMapper::toResponse)
                        .toList()
        );
    }

    //  CUSTOMER / ADMIN: all my reviews or admin list
    @GetMapping("/reviews")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Get reviews",
            description = "mine=true returns reviews created by the logged-in user. " +
                    "Admins receive all reviews."
    )
    @ApiResponse(responseCode = "200", description = "Reviews retrieved")
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @Parameter(description = "Fetch only logged-in user's reviews", example = "false")
            @RequestParam(defaultValue = "false") boolean mine,
            Authentication authentication
    ) {
        if (mine) {
            Long userId = (Long) authentication.getPrincipal();
            return ResponseEntity.ok(
                    reviewService.getReviewsByUser(userId)
                            .stream()
                            .map(ReviewMapper::toResponse)
                            .toList()
            );
        }

        return ResponseEntity.ok(
                reviewService.getAllReviews()
                        .stream()
                        .map(ReviewMapper::toResponse)
                        .toList()
        );
    }

    //  ADMIN: get review by id
    @GetMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get review by ID",
            description = "Fetch specific review for moderation or inspection."
    )
    @ApiResponse(responseCode = "200", description = "Review retrieved")
    public ResponseEntity<ReviewResponse> getReviewById(
            @Parameter(description = "Review ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ReviewMapper.toResponse(
                        reviewService.getReviewById(id)
                )
        );
    }

    //  ADMIN: approve or reject review (PENDING only)
    @PutMapping("/reviews/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update review status",
            description = "Approve or reject a pending review."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    public ResponseEntity<ReviewResponse> updateReviewStatus(
            @Parameter(description = "Review ID") @PathVariable Long id,
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New review status",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ReviewStatusUpdateRequest.class)
                    )
            )
            ReviewStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ReviewMapper.toResponse(
                        reviewService.updateReviewStatus(id, request.getStatus())
                )
        );
    }

    //  CUSTOMER (owner) or ADMIN: update review content
    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Update review content",
            description = "Customer edits reset status to PENDING. Admin edits keep status."
    )
    @ApiResponse(responseCode = "200", description = "Review updated")
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                ReviewMapper.toResponse(
                        reviewService.updateReview(
                                id,
                                ReviewMapper.toEntity(request),
                                userId,
                                isAdmin
                        )
                )
        );
    }

    //  CUSTOMER (owner) or ADMIN: delete review
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Delete review",
            description = "Deletes review owned by user or by admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted"),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        reviewService.deleteReview(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}