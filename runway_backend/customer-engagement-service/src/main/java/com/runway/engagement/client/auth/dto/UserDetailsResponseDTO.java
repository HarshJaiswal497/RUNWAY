package com.runway.engagement.client.auth.dto;

import lombok.Data;

@Data
public class UserDetailsResponseDTO {

    private boolean success;
    private User user;

    @Data
    public static class User {
        private Long user_id;
        private String email;
    }
}