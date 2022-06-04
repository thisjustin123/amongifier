package com.thisjustin.amongifier.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class AmongifierConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/amongifier/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Origin", "Content-Type", "Accept")
                .allowCredentials(true).maxAge(3600);
    }
}
