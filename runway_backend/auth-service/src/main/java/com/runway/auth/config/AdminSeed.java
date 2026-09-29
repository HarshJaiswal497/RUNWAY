package com.runway.auth.config;

import com.runway.auth.dao.UserDao;
import com.runway.auth.model.User;
import com.runway.auth.type.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class AdminSeed {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    CommandLineRunner seedAdmin(UserDao userDao, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userDao.getUserByUsername(adminUsername) == null) {
                User admin = User.builder()
                        .name("ADMIN")
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .gender("male")
                        .roles(Set.of(Role.ADMIN))
                        .build();

                userDao.saveUser(admin);
                System.out.println("Admin user seeded successfully");
            } else {
                System.out.println("Admin user already exists");
            }
        };
    }
}