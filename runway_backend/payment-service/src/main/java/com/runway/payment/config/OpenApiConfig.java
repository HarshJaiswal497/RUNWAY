package com.runway.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH = "bearerAuth";
	private static final String USER_ID_HEADER = "userIdHeader";
	private static final String USER_ROLE_HEADER = "userRoleHeader";

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(
				new Info()
					.title("Payment Service API")
					.version("1.0.0")
					.description("Payment Service for Online Shopping App")
					.contact(new Contact().name("Team"))
			)
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
			.addSecurityItem(new SecurityRequirement().addList(USER_ID_HEADER))
			.addSecurityItem(new SecurityRequirement().addList(USER_ROLE_HEADER))
			.components(
				new Components()
					.addSecuritySchemes(
						BEARER_AUTH,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
					)
					.addSecuritySchemes(
						USER_ID_HEADER,
						new SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.in(SecurityScheme.In.HEADER)
							.name("X-User-Id")
					)
					.addSecuritySchemes(
						USER_ROLE_HEADER,
						new SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.in(SecurityScheme.In.HEADER)
							.name("X-User-Role")
					)
			)
			.servers(List.of(new Server().url("http://localhost:9191").description("API Gateway")));
	}
}
