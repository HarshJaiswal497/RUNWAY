package com.runway.auth.dto;

import com.runway.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class UserDetailsResponseDTO {

	private boolean success;
	private User user;
}

