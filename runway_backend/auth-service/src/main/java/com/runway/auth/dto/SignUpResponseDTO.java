package com.runway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignUpResponseDTO {

    private boolean success;
    private String username;
    private long userId;
}

