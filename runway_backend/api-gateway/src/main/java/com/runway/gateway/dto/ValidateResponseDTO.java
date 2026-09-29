package com.runway.gateway.dto;

import com.runway.gateway.type.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateResponseDTO {

    private boolean valid;
    private long userId;
    private String role;
}

