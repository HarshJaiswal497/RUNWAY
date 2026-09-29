package com.runway.engagement.client.auth;

import com.runway.engagement.client.auth.dto.UserDetailsResponseDTO;
import com.runway.engagement.client.auth.dto.ValidateResponseDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "AUTH-SERVICE",
        fallback = AuthClient.AuthClientFallback.class
)
public interface AuthClient {

    @PostMapping("/auth/validate")
    ValidateResponseDTO validateToken(@RequestBody String token);

    @GetMapping("/auth/userdetails")
    UserDetailsResponseDTO getUserDetails(@RequestParam long userId);

    @Component
    class AuthClientFallback implements AuthClient {

        @Override
        public ValidateResponseDTO validateToken(String token) {
            ValidateResponseDTO fallback = new ValidateResponseDTO();
            fallback.setValid(false);
            fallback.setUserId(null);
            return fallback;
        }

        @Override
        public UserDetailsResponseDTO getUserDetails(long userId) {

            UserDetailsResponseDTO fallback = new UserDetailsResponseDTO();
            fallback.setSuccess(false);

            UserDetailsResponseDTO.User user = new UserDetailsResponseDTO.User();
            user.setUser_id(null);
            user.setEmail(null);

            fallback.setUser(user);

            return fallback;
        }
    }
}