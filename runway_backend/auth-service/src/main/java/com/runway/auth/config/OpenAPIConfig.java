package com.runway.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

	@Bean
	public OpenAPI customerOpenAPI() {
		return new OpenAPI()
			.info(
				new Info()
					.title("Auth Service API")
					.version("1.0.0")
					.description(
						"Authentication and Authorization Service for E-Commerce Microservices Platform. " +
						"This service handles user registration, login, JWT token management, and role-based access control."
					)
			)
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.components(
				new Components()
					.addSecuritySchemes(
						"bearerAuth",
						new SecurityScheme()
							.type(SecurityScheme.Type.HTTP)
							.scheme("bearer")
							.bearerFormat("JWT")
							.description("Enter JWT token obtained from /auth/login endpoint")
					)
					.addSecuritySchemes(
						"userId",
						new SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.in(SecurityScheme.In.HEADER)
							.name("X-USER-ID")
							.description("User ID header for specific operations")
					)
					.addSecuritySchemes(
						"userRole",
						new SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.in(SecurityScheme.In.HEADER)
							.name("X-User-Role")
							.description("User role header (ADMIN, SELLER, SUPPORT, CUSTOMER)")
					)
			)
                .servers(List.of(new Server().url("http://localhost:9191").description("API Gateway")));
	}
}

