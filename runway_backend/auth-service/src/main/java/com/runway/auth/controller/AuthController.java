package com.runway.auth.controller;

import com.runway.auth.dto.*;
import com.runway.auth.serviceImpl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and account management endpoints")
public class AuthController {

	private final AuthServiceImpl authServiceImpl;

	@PostMapping("/login")
	@Operation(summary = "User Login", description = "Authenticate user and generate JWT token")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Login successful"),
		@ApiResponse(responseCode = "401", description = "Invalid credentials")
	})
	@SecurityRequirement(name = "") // No security required
	public ResponseEntity<LoginResponseDTO> login(
		@RequestBody LoginRequestDTO loginRequestDTO,
		HttpServletResponse response
	) {
		return ResponseEntity.ok(authServiceImpl.login(loginRequestDTO, response));
	}

	@PostMapping("/signup")
	@Operation(summary = "User Registration", description = "Register a new user account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "User registered successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input data"),
		@ApiResponse(responseCode = "409", description = "User already exists")
	})
	@SecurityRequirement(name = "") // No security required
	public ResponseEntity<SignUpResponseDTO> signup(
		@RequestBody SignUpRequestDTO signUpRequestDTO,
		HttpServletResponse response
	) throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED).body(authServiceImpl.signup(signUpRequestDTO));
	}


	@PostMapping("/refreshtoken")
	@Operation(summary = "Refresh Token", description = "Generate a new JWT token using refresh token")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
		@ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
	})
	public ResponseEntity<LoginResponseDTO> refreshToken(HttpServletRequest request) {
		return ResponseEntity.ok(authServiceImpl.refresh(request));
	}

	@PostMapping("/validate")
	@Operation(summary = "Validate Token", description = "Validate JWT token and return user details")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Token is valid"),
		@ApiResponse(responseCode = "401", description = "Invalid or expired token")
	})
	@SecurityRequirement(name = "") // No security required
	public ResponseEntity<ValidateResponseDTO> validateToken(@RequestBody String token) {
		return ResponseEntity.ok(authServiceImpl.validateToken(token));
	}

    @GetMapping("/users")
    @Operation(summary = "Get All Users", description = "Admin only - retrieve all users, optionally filtered by role")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserDetailsResponseDTO>> getAllUsers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String filterRole
    ) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(authServiceImpl.getAllUsers(filterRole));
    }

	@GetMapping("/userdetails")
	@Operation(summary = "Get User Details", description = "Retrieve user details by user ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "User not found")
	})
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<UserDetailsResponseDTO> getUserDetails(@RequestParam long userId) {
		return ResponseEntity.ok(authServiceImpl.getUserDetails(userId));
	}

	@DeleteMapping("/deleteaccount")
	@Operation(summary = "Delete Account", description = "Delete user account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Account deleted successfully"),
		@ApiResponse(responseCode = "404", description = "User not found"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<DeleteUserResponseDTO> deleteAccount(
		@RequestBody DeleteAccountRequestDTO requestDTO
	) {
		return ResponseEntity.ok(authServiceImpl.deleteAccount(requestDTO));
	}
}

