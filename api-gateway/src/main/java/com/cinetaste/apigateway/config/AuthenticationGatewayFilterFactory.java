// api-gateway/src/main/java/com/cinetaste/apigateway/config/AuthenticationGatewayFilterFactory.java

package com.cinetaste.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;
import org.springframework.http.server.reactive.ServerHttpRequest;

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

            // ===== QUY TẮC PUBLIC ENDPOINTS =====
            List<Predicate<ServerHttpRequest>> publicEndpoints = List.of(
                    // Auth endpoints
                    r -> r.getURI().getPath().startsWith("/api/auth/"),

                    // User public endpoints
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().matches("/api/users/[^/]+$"),

                    // ✅ THÊM: Cho phép Recipe Service gọi User Service (internal call)
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().matches("/api/users/[^/]+/basic-info$"),

                    // Recipe public endpoints
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().matches("/api/recipes(/[^/]+)?$"),
                    r -> r.getMethod().equals(HttpMethod.GET) && r.getURI().getPath().matches("/api/recipes/[^/]+/comments$"),

                    // Health check
                    r -> r.getURI().getPath().startsWith("/actuator/")
            );

            // Kiểm tra xem request có khớp với bất kỳ endpoint công khai nào không
            boolean isPublic = publicEndpoints.stream().anyMatch(p -> p.test(request));

            if (isPublic) {
                System.out.println("✅ Public endpoint: " + request.getURI().getPath());
                return chain.filter(exchange);
            }

            // ===== KIỂM TRA TOKEN CHO PROTECTED ENDPOINTS =====
            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                System.err.println("❌ Missing Authorization header for: " + request.getURI().getPath());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.err.println("❌ Invalid Authorization header format");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (jwtUtil.isTokenExpired(token)) {
                    System.err.println("❌ Token expired");
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                String userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", String.class));

                System.out.println("✅ Authenticated user: " + userId + " for path: " + request.getURI().getPath());

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(builder -> builder.header("X-User-ID", userId))
                        .build();

                return chain.filter(modifiedExchange);

            } catch (Exception e) {
                System.err.println("❌ Token validation failed: " + e.getMessage());
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