package com.runway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class SignUpRequestDTO {
    private String username;
    private String password;
    private String name;
    private String gender;
    private String email;
}

