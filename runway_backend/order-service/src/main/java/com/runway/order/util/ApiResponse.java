package com.runway.order.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ApiResponse — Standard JSON wrapper for all API responses.
 *
 * Success: { "success": true,  "message": "...", "data": {...}, "timestamp": "..." }
 * Error  : { "success": false, "message": "...",               "timestamp": "..." }
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean       success;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true).message(message).data(data)
            .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true).message(message)
            .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false).message(message)
            .timestamp(LocalDateTime.now()).build();
    }
}
