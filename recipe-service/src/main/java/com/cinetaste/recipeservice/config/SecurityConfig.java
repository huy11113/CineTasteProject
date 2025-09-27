// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/config/SecurityConfig.java
package com.cinetaste.recipeservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Thêm import này
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // **DÒNG QUAN TRỌNG:**
                        // Cho phép TẤT CẢ request GET đến /api/recipes và các đường dẫn con của nó
                        .requestMatchers(HttpMethod.GET, "/api/recipes/**").permitAll()

                        // Tất cả các request còn lại (POST, PUT, DELETE...) đều cần phải được xác thực
                        .anyRequest().authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/rating/**").permitAll()

                        // Tất cả các request còn lại (POST, PUT, DELETE...) đều cần phải được xác thực
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}