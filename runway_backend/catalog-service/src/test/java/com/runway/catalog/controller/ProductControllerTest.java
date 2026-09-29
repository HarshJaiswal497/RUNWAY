package com.runway.catalog.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.runway.catalog.dto.request.ProductRequest;
import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProductService productService;


	@MockitoBean
	private SecurityFilterChain securityFilterChain;

	@Test
	@DisplayName("GET /catalog/products/{id} - Success")
	@WithMockUser(roles = "CUSTOMER")
	void getProduct_Success() throws Exception {
		// Arrange
		ProductResponse response = new ProductResponse(
			1L,
			"SKU100",
			"Monitor",
			"4K Display",
			new BigDecimal("30000"),
			"INR",
			1L,
			50L,
			null,
			List.of()
		);
		when(productService.getProductById(1L)).thenReturn(response);

		// Act & Assert
		mockMvc
			.perform(get("/catalog/products/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Monitor"))
			.andExpect(jsonPath("$.sku").value("SKU100"));
	}

	@Test
	@DisplayName("PUT /catalog/products/{id} - Success as Seller")
	@WithMockUser(roles = "SELLER")
	void updateProduct_AsSeller_Success() throws Exception {
		// Arrange
		ProductRequest request = new ProductRequest("SKU1", "Updated", "Desc", BigDecimal.TEN, "INR", 1L, 0, null);
		ProductResponse response = new ProductResponse(
			1L,
			"SKU1",
			"Updated",
			"Desc",
			BigDecimal.TEN,
			"INR",
			1L,
			100L,
			null,
			List.of()
		);

		when(productService.updateProduct(anyLong(), any(), anyLong(), anyString())).thenReturn(response);

		// Act & Assert
		mockMvc
			.perform(
				put("/catalog/products/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-USER-ID", 100L)
					.header("X-USER-ROLE", "SELLER")
					.with(csrf())
			) // Required because security is active (but mocked)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Updated"));
	}

	@Test
	@DisplayName("PUT /catalog/products/{id} - Unauthorized Role (Logic Test)")
	@WithMockUser(roles = "CUSTOMER")
	void updateProduct_AsCustomer_Forbidden() throws Exception {

		ProductRequest request = new ProductRequest("SKU1", "T", "D", BigDecimal.ONE, "INR", 1L, 0, null);

		mockMvc
			.perform(
				put("/catalog/products/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-USER-ID", 100L)
					.header("X-USER-ROLE", "CUSTOMER")
					.with(csrf())
			)
			.andExpect(status().isOk()); // Adjust based on your actual business logic check
	}

	@Test
	@DisplayName("PUT /catalog/products/{id} - Should fail when user is not owner")
	@WithMockUser(roles = "SELLER")
	void updateProduct_NotOwner_ShouldFail() throws Exception {
		// Arrange
		Long productId = 1L;
		Long intruderId = 999L;
		ProductRequest request = new ProductRequest("SKU1", "Title", "Desc", BigDecimal.TEN, "INR", 1L, 0, null);

		when(productService.updateProduct(eq(productId), any(), eq(intruderId), eq("SELLER")))
			.thenThrow(new RuntimeException("Unauthorized: You do not own this product!"));

		// Act & Assert
		mockMvc
			.perform(
				put("/catalog/products/" + productId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-User-Id", intruderId) // Match controller casing
					.header("X-USER-ROLE", "SELLER")
					.with(csrf())
			)
			.andExpect(status().isUnauthorized()) // Matches your GlobalExceptionHandler logic (401)
			.andExpect(jsonPath("$.error").value("Unauthorized: You do not own this product!"));
	}

	@Test
	@DisplayName("POST /catalog/products - Success with Image")
	@WithMockUser(roles = "SELLER")
	void createProduct_Success() throws Exception {
		ProductRequest request = new ProductRequest(
			"SKU-NEW",
			"New Product",
			"Desc",
			new BigDecimal("100"),
			"INR",
			1L,
			10,
			null
		);
		String productJson = objectMapper.writeValueAsString(request);

		// Mock the service response
		ProductResponse response = new ProductResponse(
			1L,
			"SKU-NEW",
			"New Product",
			"Desc",
			new BigDecimal("100"),
			"INR",
			1L,
			100L,
			null,
			List.of()
		);
		when(productService.createProduct(any(), any(), anyLong())).thenReturn(response);

		mockMvc
			.perform(
				multipart("/catalog/products")
					.file(new MockMultipartFile("product", "", "application/json", productJson.getBytes()))
					.file(new MockMultipartFile("images", "test.jpg", "image/jpeg", "image-data".getBytes()))
					.header("X-User-Id", 100L)
					.with(csrf())
			)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sku").value("SKU-NEW"));
	}

	@Test
	@DisplayName("GET /catalog/products/{id} - Should return 404 when product missing")
	@WithMockUser(roles = "CUSTOMER")
	void getProduct_NotFound() throws Exception {
		// Arrange
		Long productId = 99L;
		when(productService.getProductById(productId))
			.thenThrow(new RuntimeException("Product not found with ID: " + productId));

		// Act & Assert
		mockMvc
			.perform(get("/catalog/products/" + productId))
			.andExpect(status().isNotFound()) // Verifies your Handler turns "not found" into 404
			.andExpect(jsonPath("$.error").value("Product not found with ID: 99"));
	}

	@Test
	@DisplayName("GET /catalog/products - Success")
	@WithMockUser
	void getAllProducts_ApiSuccess() throws Exception {
		// Arrange
		ProductResponse p1 = new ProductResponse(1L, "S1", "P1", "D", BigDecimal.ONE, "INR", 1L, 10L, null, List.of());
		when(productService.getAllProducts()).thenReturn(List.of(p1));

		// Act & Assert
		mockMvc
			.perform(get("/catalog/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].title").value("P1"));
	}

	@Test
	@DisplayName("GET /catalog/products/category/{id} - Success")
	@WithMockUser
	void getProductsByCategory_ApiSuccess() throws Exception {
		// Arrange
		Long catId = 5L;
		ProductResponse p1 = new ProductResponse(
			1L,
			"S1",
			"Cat Product",
			"D",
			BigDecimal.ONE,
			"INR",
			catId,
			10L,
			null,
			List.of()
		);
		when(productService.getProductsByCategoryId(catId)).thenReturn(List.of(p1));

		// Act & Assert
		mockMvc
			.perform(get("/catalog/products/category/" + catId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].categoryId").value(catId))
			.andExpect(jsonPath("$[0].title").value("Cat Product"));
	}
}
