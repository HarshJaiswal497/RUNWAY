package com.runway.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SwaggerConfig — OpenAPI 3 Documentation
 * Visit: http://localhost:2000/swagger-ui.html
 * Click "Authorize" and paste Bearer token to test protected APIs.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Service API")
                .description(
                    "Microservice for Cart, Order, and Shipment management.\n\n" +
                    "**Auth:** Get JWT from Identity Service → Click Authorize → " +
                    "Enter: `Bearer <token>`\n\n" +
                    "**Port:** 2000 | **DB:** order_db"
                )
                .version("1.0.0")
                .contact(new Contact()
                    .name("Ecom_Order Service")
                    .email("team@runway.com")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .servers(List.of(new Server().url("http://localhost:9191").description("API Gateway")));
    }
}
