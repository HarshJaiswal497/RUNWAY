package com.runway.auth.dto;

import com.runway.auth.model.Support;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnBoardSupportResponseDTO {

	private boolean success;
	private Support support;
}

