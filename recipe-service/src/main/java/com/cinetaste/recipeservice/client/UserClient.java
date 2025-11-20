package com.cinetaste.recipeservice.client;

import com.cinetaste.recipeservice.dto.client.UserBasicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    // Gọi sang User Service để lấy thông tin
    public UserBasicInfo getUserById(UUID userId) {
        if (userId == null) return null;

        try {
            // URL này trỏ đến service name 'user-service' trong Docker network
            String url = "http://user-service:8081/api/users/" + userId + "/basic-info";

            return webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(UserBasicInfo.class)
                    .block(); // Gọi đồng bộ
        } catch (Exception e) {
            // Fallback: Trả về user ẩn danh nếu lỗi kết nối
            UserBasicInfo fallback = new UserBasicInfo();
            fallback.setId(userId);
            fallback.setDisplayName("Unknown Chef");
            return fallback;
        }
    }
}