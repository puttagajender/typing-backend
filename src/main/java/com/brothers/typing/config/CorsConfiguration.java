package com.brothers.typing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private static final String LOCAL_FRONTEND = "http://localhost:5173";

    private final String productionFrontendUrl;

    public CorsConfiguration(@Value("${FRONTEND_URL:}") String productionFrontendUrl) {
        this.productionFrontendUrl = productionFrontendUrl.trim();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        Set<String> allowedOrigins = new LinkedHashSet<>();
        allowedOrigins.add(LOCAL_FRONTEND);
        if (!productionFrontendUrl.isBlank() && !"*".equals(productionFrontendUrl)) {
            allowedOrigins.add(productionFrontendUrl);
        }

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type");
    }
}
