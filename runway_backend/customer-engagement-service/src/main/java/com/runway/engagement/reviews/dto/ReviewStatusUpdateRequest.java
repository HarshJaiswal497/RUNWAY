package com.runway.engagement.reviews.dto;

import com.runway.engagement.reviews.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public class ReviewStatusUpdateRequest {

    @NotNull
    private ReviewStatus status;

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }
}