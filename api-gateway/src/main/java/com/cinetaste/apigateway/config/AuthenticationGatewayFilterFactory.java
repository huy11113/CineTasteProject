package com.cinetaste.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod; // Thêm import này
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate; // Thêm import này
import org.springframework.http.server.reactive.ServerHttpRequest; // Thêm import này


@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // --- QUY TẮC MỚI ĐƯỢC CẬP NHẬT ---
            // Danh sách các endpoints công khai
            List<Predicate<ServerHttpRequest>> publicEndpoints = List.of(
                    r -> r.getURI().getPath().startsWith("/api/auth/"),
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().matches("/api/users/[^/]+$"),
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().startsWith("/api/recipes") // Cho phép xem công thức và bình luận
            );

            // Kiểm tra xem request có khớp với bất kỳ endpoint công khai nào không
            boolean isPublic = publicEndpoints.stream().anyMatch(p -> p.test(request));

            if (isPublic) {
                return chain.filter(exchange); // Nếu là public, cho qua luôn
            }
            // --- KẾT THÚC CẬP NHẬT ---


            // Nếu không phải public, thực hiện kiểm tra token như cũ
            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (jwtUtil.isTokenExpired(token)) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                String userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", String.class));

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(builder -> builder.header("X-User-ID", userId))
                        .build();

                return chain.filter(modifiedExchange);

            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties
    }
}