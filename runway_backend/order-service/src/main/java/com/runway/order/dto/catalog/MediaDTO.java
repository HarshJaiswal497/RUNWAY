package com.runway.order.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mirroring Catalog Service's MediaResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private Long id;
    private String fullUrl;
    private boolean isPrimary;
    private Integer sortOrder;
}
