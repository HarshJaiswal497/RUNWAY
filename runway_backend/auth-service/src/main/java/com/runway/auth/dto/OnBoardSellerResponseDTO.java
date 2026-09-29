package com.runway.auth.dto;

import com.runway.auth.model.Seller;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnBoardSellerResponseDTO {

	private boolean success;
	private Seller seller;
}

