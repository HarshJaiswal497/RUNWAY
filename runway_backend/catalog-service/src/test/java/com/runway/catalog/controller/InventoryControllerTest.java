package com.runway.catalog.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.runway.catalog.dto.response.InventoryResponse;
import com.runway.catalog.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InventoryService inventoryService;

	@MockitoBean
	private SecurityFilterChain securityFilterChain;

	@Test
	@DisplayName("GET /catalog/inventory/{id} - Success")
	@WithMockUser
	void getStock_Success() throws Exception {
		when(inventoryService.getStockForProduct(1L)).thenReturn(50);

		mockMvc
			.perform(get("/catalog/inventory/1"))
			.andExpect(status().isOk())
			.andExpect(content().string("50"));
	}

	@Test
	@DisplayName("PATCH /catalog/inventory/{id}/update - Unauthorized Error Handler")
	@WithMockUser(roles = "SELLER")
	void updateStock_Unauthorized_HandlerCheck() throws Exception {
		// Mocking the service to throw the exception your GlobalExceptionHandler looks for
		when(inventoryService.getStockForProduct(1L)).thenReturn(50);

		// Simulating the ownership failure
		doThrow(new RuntimeException("Unauthorized: You do not own the product!"))
			.when(inventoryService)
			.updateStock(1L, 10, 999L, "SELLER");

		mockMvc
			.perform(
				patch("/catalog/inventory/1/update")
					.param("amount", "10")
					.header("X-USER-ID", 999L)
					.header("X-USER-ROLE", "SELLER")
					.with(csrf())
			)
			.andExpect(status().isUnauthorized()) // Verification of your Exception Handler
			.andExpect(jsonPath("$.error").value("Unauthorized: You do not own the product!"));
	}
}
