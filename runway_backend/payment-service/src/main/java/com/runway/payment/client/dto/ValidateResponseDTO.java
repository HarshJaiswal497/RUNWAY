package com.runway.payment.client.dto;

public class ValidateResponseDTO {

    private boolean valid;
    private long userId;
    private String role; // Using String to avoid depending on auth-service's Role enum

    public ValidateResponseDTO() {}

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

