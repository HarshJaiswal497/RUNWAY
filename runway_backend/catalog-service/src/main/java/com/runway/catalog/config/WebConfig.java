package com.runway.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the URL path to the physical folder
        registry.addResourceHandler("/api/v1/catalog/images/**")
                .addResourceLocations("file:" + uploadDir);
    }
    //causing errors becauses cors config is present in gateway and also in this service
    // for frontend to access this services endpoints
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // Allow all endpoints
//                .allowedOrigins("*") // In production, we will change this to our UI URL
//                .allowedMethods("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS")
//                .allowedHeaders("*");
//    }
}