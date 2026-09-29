package com.runway.auth.dto;

import com.runway.auth.type.Role;
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

