package com.cinetaste.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    // Cho phép CORS từ localhost:3000 (địa chỉ frontend của bạn)
    private final String allowedOrigin = "http://localhost:3000";

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Chỉ cho phép origin cụ thể này
        corsConfig.setAllowedOrigins(Collections.singletonList(allowedOrigin));

        corsConfig.setMaxAge(3600L); // Thời gian cache preflight request (giây)
        corsConfig.addAllowedMethod("*"); // Cho phép tất cả các method (GET, POST, etc.)
        corsConfig.addAllowedHeader("*"); // Cho phép tất cả các header
        corsConfig.setAllowCredentials(true); // Cho phép gửi cookie/credentials

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // Áp dụng cho tất cả đường dẫn

        return new CorsWebFilter(source);
    }
}