package com.thisjustin.amongifier.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AmongifierConfiguration {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new AmongifierConfigurer();
    }
}
