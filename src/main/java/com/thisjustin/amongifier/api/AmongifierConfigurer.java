package com.thisjustin.amongifier.api;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class AmongifierConfigurer implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/amongifier/**")
                .allowedOrigins("http://localhost:3000", "https://thisjustin123.github.io/amongifier/")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Origin", "Content-Type", "Accept")
                .allowCredentials(true).maxAge(3600);
    }
}
