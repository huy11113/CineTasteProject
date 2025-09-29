package com.cinetaste.recipeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF (Cross-Site Request Forgery) vì chúng ta dùng API stateless
                .csrf(csrf -> csrf.disable())

                // Cấu hình quy tắc cho các request HTTP
                .authorizeHttpRequests(auth -> auth
                        // Cho phép TẤT CẢ các request đi vào service này mà không cần kiểm tra quyền
                        .anyRequest().permitAll()
                )

                // Cấu hình quản lý session: không tạo session vì chúng ta dùng token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}