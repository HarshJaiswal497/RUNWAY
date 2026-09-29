package com.runway.auth.controller;

import com.runway.auth.dto.*;
import com.runway.auth.serviceImpl.AdminServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Tag(name = "Admin Management", description = "Admin operations for user onboarding and management")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "userRole")
public class AdminController {

    private final AdminServiceImpl adminServiceImpl;

    @PostMapping("/onboardseller")
    @Operation(summary = "Onboard Seller", description = "Admin operation to onboard a new seller")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Seller onboarded successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OnBoardSellerResponseDTO> onboardSeller(
            @Parameter(description = "User role (must be ADMIN)", required = true)
            @RequestHeader("X-User-Role") String role,
            @RequestBody OnBoardRequest requestDTO) {

        // 1. Role-Based Access Control (RBAC) check
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. Call Service logic
        OnBoardSellerResponseDTO response = adminServiceImpl.onboardSeller(requestDTO);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/onboardsupport")
    @Operation(summary = "Onboard Support", description = "Admin operation to onboard a new support user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Support user onboarded successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OnBoardSupportResponseDTO> onboardSupport(
            @Parameter(description = "User role (must be ADMIN)", required = true)
            @RequestHeader("X-User-Role") String role,
            @RequestBody OnBoardRequest requestDTO) {

        // 1. Role-Based Access Control (RBAC) check
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. Call Service logic
        OnBoardSupportResponseDTO response = adminServiceImpl.onboardSupport(requestDTO);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/offboard")
    @Operation(summary = "Offboard User", description = "Admin operation to offboard user to customer role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User offboarded successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<OffboardResponseDTO> offboardToCustomer(
            @Parameter(description = "User role (must be ADMIN)", required = true)
            @RequestHeader("X-User-Role") String role,
            @RequestBody OnBoardRequest requestDTO) {

        // 1. Role-Based Access Control (RBAC) check
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. Call Service logic
        OffboardResponseDTO response = adminServiceImpl.offboardToCustomer(requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteuser/{userId}")
    @Operation(summary = "Delete User", description = "Admin operation to delete a user by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<DeleteUserResponseDTO> deleteUserById(
            @Parameter(description = "User role (must be ADMIN)", required = true)
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "User ID to delete", required = true)
            @PathVariable long userId) {

        // 1. Role-Based Access Control (RBAC) check
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. Call Service logic
        DeleteUserResponseDTO response = adminServiceImpl.deleteUserById(userId);
        return ResponseEntity.ok(response);
    }
}
