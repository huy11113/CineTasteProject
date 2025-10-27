package com.cinetaste.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Tắt CSRF một cách triệt để tại Gateway
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Cho phép tất cả các request đi qua lớp bảo mật này
                // (vì việc xác thực token đã được xử lý bởi AuthenticationFilter)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().permitAll()

                );
        return http.build();
    }
}