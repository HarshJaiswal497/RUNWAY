package com.runway.gateway.config;

import com.runway.gateway.dto.ValidateResponseDTO;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, WebClient.Builder webClientBuilder) {
		return builder
			.routes()
                .route("auth_userdetails_public",
                        r -> r.path("/auth/userdetails")
                                .and().method("GET")
                                .filters(f -> f.dedupeResponseHeader(
                                        "Access-Control-Allow-Origin", "RETAIN_FIRST"
                                ))
                                .uri("lb://AUTH-SERVICE")
                )
			.route(
				"auth-service-api-docs",
				r ->
					r
						.path("/service/auth-service/v3/api-docs")
						.filters(f -> f.rewritePath("/service/auth-service/(?<path>.*)", "/${path}"))
						.uri("lb://AUTH-SERVICE")
			)
			.
                route(
				"catalog-service-api-docs",
				r ->
					r
						.path("/service/catalog-service/v3/api-docs")
						.filters(f -> f.rewritePath("/service/catalog-service/(?<path>.*)", "/${path}"))
						.uri("lb://CATALOG-SERVICE")
			)
			.route(
				"order-service-api-docs",
				r ->
					r
						.path("/service/order-service/v3/api-docs")
						.filters(f -> f.rewritePath("/service/order-service/(?<path>.*)", "/${path}"))
						.uri("lb://ORDER-SERVICE")
			)
			.route(
				"customer-engagement-service-api-docs",
				r ->
					r
						.path("/service/customer-engagement-service/v3/api-docs")
						.filters(f -> f.rewritePath("/service/customer-engagement-service/(?<path>.*)", "/${path}"))
						.uri("lb://CUSTOMER-ENGAGEMENT-SERVICE")
			)
			.route(
				"payment-service-api-docs",
				r ->
					r
						.path("/service/payment-service/v3/api-docs")
						.filters(f -> f.rewritePath("/service/payment-service/(?<path>.*)", "/${path}"))
						.uri("lb://PAYMENT-SERVICE")
			)
			// BUSINESS ROUTES
			// 1. PUBLIC AUTH ROUTES (Login/Signup - No Filter needed)
                // Public user details endpoint
                .route("auth_userdetails_public",
                        r -> r.path("/auth/userdetails")
                                .and().method("GET")
                                .uri("lb://AUTH-SERVICE")
                )
			.route("auth_public_route", r -> r.path("/auth/login", "/auth/signup").uri("lb://AUTH-SERVICE"))
			// 2. PROTECTED ADMIN ROUTES (Needs Header Injection)
			.route(
				"admin_service_route",
				r -> r.path("/admin/**").filters(f -> f.filter(authFilter(webClientBuilder))).uri("lb://AUTH-SERVICE")
			)
			// 3. CATALOG ROUTES
                //wishlist routes (Protected - User only)
                .route("catalog_wishlist_route",
                        r -> r.path("/catalog/wishlist/**")
                                .filters(f -> f.filter(authFilter(webClientBuilder)))
                                .uri("lb://CATALOG-SERVICE")
                )
                // 3a. PUBLIC CATALOG ROUTES (No auth needed)
                .route(
                        "catalog_public_route",
                        r ->
                                r
                                        .path(
                                                "/catalog/products",
                                                "/catalog/products/{id}",
                                                "/catalog/products/category/{categoryId}",
                                                "/catalog/products/seller/{sellerId}",
                                                "/catalog/categories",
                                                "/catalog/categories/all",
                                                "/catalog/inventory/{productId}"
                                        )
                                        .and()
                                        .method("GET")
                                        .uri("lb://CATALOG-SERVICE")
                )
                // 3b. PROTECTED CATALOG ROUTES (Seller/Admin only)
                .route(
                        "catalog_protected_route",
                        r ->
                                r
                                        .path("/catalog/**")
                                        .filters(f -> f.filter(authFilter(webClientBuilder)))
                                        .uri("lb://CATALOG-SERVICE")
                )
			// 4. PROTECTED ORDER ROUTES
			.route(
				"order_service_route",
				r ->
					r
						.path("/api/cart/**", "/api/orders/**", "/api/shipments/**")
						.filters(f -> f.filter(authFilter(webClientBuilder)))
						.uri("lb://ORDER-SERVICE")
			)
                // 5. PROTECTED PAYMENT ROUTES
                .route(
                        "payment_service_route",
                        r ->
                                r
                                        .path("/payment/**")
                                        .filters(f -> f.filter(authFilter(webClientBuilder)))
                                        .uri("lb://PAYMENT-SERVICE")
                )
// 6a. PUBLIC CUSTOMER ENGAGEMENT ROUTES ← move this UP
                .route(
                        "customer_engagement_public_route",
                        r ->
                                r
                                        .path(
                                                "/products/{productId}/ratings",
                                                "/products/{productId}/ratings/average",
                                                "/products/{productId}/reviews"
                                        )
                                        .and()
                                        .method("GET")
                                        .uri("lb://CUSTOMER-ENGAGEMENT-SERVICE")
                )
// 6b. PROTECTED CUSTOMER ENGAGEMENT ROUTES ← this comes AFTER
                .route(
                        "customer_engagement_service_route",
                        r ->
                                r
                                        .path(
                                                "/products/**",
                                                "/reviews/**",
                                                "/ratings/**",
                                                "/notification-templates/**",
                                                "/notification-logs/**",
                                                "/tickets/**"
                                        )
                                        .filters(f -> f.filter(authFilter(webClientBuilder)))
                                        .uri("lb://CUSTOMER-ENGAGEMENT-SERVICE")
                )
                .build();
	}

	//  Reusable filter logic to validate tokens and inject X-User-Id and X-User-Role headers.
	private GatewayFilter authFilter(WebClient.Builder webClientBuilder) {
		return (exchange, next) -> {
			String path = exchange.getRequest().getURI().getPath();
			System.out.println("[GATEWAY] Intercepting request to: " + path);

			// Extract Token
			String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				System.out.println("[GATEWAY] No Bearer token found for: " + path);
				exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
				return exchange.getResponse().setComplete();
			}

			String token = authHeader.substring(7);

			// Reactive Call to Auth-Service
			return webClientBuilder
				.build()
				.post()
				.uri("http://AUTH-SERVICE/auth/validate")
				.bodyValue(token)
				.retrieve()
				.bodyToMono(ValidateResponseDTO.class)
				.flatMap(response -> {
					if (response != null && response.isValid()) {
						System.out.println("[GATEWAY] Token Valid for " + path + ". UserID: " + response.getUserId());

						// Inject headers into the request before forwarding
						return next.filter(
							exchange
								.mutate()
								.request(builder ->
									builder
										.header("X-User-Id", String.valueOf(response.getUserId()))
										.header("X-User-Role", response.getRole())
								)
								.build()
						);
					}
					System.out.println("[GATEWAY] Invalid Token for: " + path);
					exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
					return exchange.getResponse().setComplete();
				})
				.onErrorResume(e -> {
					System.err.println("[GATEWAY] Auth-Service Error: " + e.getMessage());
					exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
					return exchange.getResponse().setComplete();
				});
		};
	}
}
