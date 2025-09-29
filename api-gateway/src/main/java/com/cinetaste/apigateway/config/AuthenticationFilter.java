// Vị trí: api-gateway/src/main/java/com/cinetaste/apigateway/config/AuthenticationFilter.java
package com.cinetaste.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    // Inject các class cần thiết, ví dụ JwtUtil

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Logic xác thực JWT ở đây
            // Nếu hợp lệ:
            //      exchange.getRequest().mutate().header("X-User-ID", userId).build();
            //      return chain.filter(exchange);
            // Nếu không hợp lệ, trả về lỗi 401

            // Tạm thời cho qua tất cả để test
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Có thể thêm các cấu hình cho filter ở đây
    }
}