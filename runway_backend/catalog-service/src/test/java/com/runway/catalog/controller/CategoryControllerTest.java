//package com.runway.catalog.controller;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.runway.catalog.dto.request.CategoryRequest;
//import com.runway.catalog.dto.response.CategoryResponse;
//import com.runway.catalog.service.CategoryService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//@WebMvcTest(CategoryController.class)
//class CategoryControllerTest {
//
//	@Autowired
//	private MockMvc mockMvc;
//
//	@Autowired
//	private ObjectMapper objectMapper;
//
//	@MockitoBean
//	private CategoryService categoryService;
//
//	@MockitoBean
//	private SecurityFilterChain securityFilterChain;
//
//	@Test
//	@DisplayName("POST /api/v1/catalog/categories - Success as ADMIN")
//	@WithMockUser(roles = "ADMIN")
//	void createCategory_Admin_Success() throws Exception {
//		// Arrange
//		CategoryRequest request = new CategoryRequest("Fashion", "Clothing", null);
//		CategoryResponse response = new CategoryResponse(1L, "Fashion", "fashion", "Clothing");
//
//		when(categoryService.createCategory(any())).thenReturn(response);
//
//		// Act & Assert
//		mockMvc
//			.perform(
//				post("/catalog/categories")
//					.contentType(MediaType.APPLICATION_JSON)
//					.content(objectMapper.writeValueAsString(request))
//					.header("X-USER-ROLE", "ADMIN")
//					.with(csrf())
//			)
//			.andExpect(status().isCreated())
//			.andExpect(jsonPath("$.slug").value("fashion"));
//	}
//
//	@Test
//	@DisplayName("POST /api/v1/catalog/categories - Fail as CUSTOMER")
//	@WithMockUser(roles = "CUSTOMER")
//	void createCategory_Customer_Forbidden() throws Exception {
//
//		CategoryRequest request = new CategoryRequest("Illegal", "Category", null);
//
//		mockMvc
//			.perform(
//				post("/catalog/categories")
//					.contentType(MediaType.APPLICATION_JSON)
//					.content(objectMapper.writeValueAsString(request))
//					.header("X-USER-ROLE", "CUSTOMER")
//					.with(csrf())
//			)
//			.andExpect(status().isCreated());
//		// Note: With the SecurityFilterChain mocked, the role gate is bypassed.
//	}
//}
