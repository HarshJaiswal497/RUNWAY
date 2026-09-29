package com.runway.engagement.client.auth.dto;

import lombok.Data;

@Data
public class ValidateResponseDTO {
    private boolean valid;
    private Long userId;
    private String role;
}