package com.runway.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Value("${gateway.url:http://localhost:9191}")
	private String serverUrl;

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info().title("Catalog Service API").version("1.0"))
			// Add Security Requirement so the "Authorize" button appears
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.addSecurityItem(new SecurityRequirement().addList("userIdHeader"))
			.components(
				new Components()
					.addSecuritySchemes(
						"bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
					)
					.addSecuritySchemes(
						"userIdHeader",
						new SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.in(SecurityScheme.In.HEADER)
							.name("X-USER-ID")
					)
			)
			.servers(List.of(new Server().url(serverUrl).description("API Gateway")));
	}
}
