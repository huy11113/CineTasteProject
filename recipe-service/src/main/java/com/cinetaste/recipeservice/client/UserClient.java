// recipe-service/src/main/java/com/cinetaste/recipeservice/client/UserClient.java

package com.cinetaste.recipeservice.client;

import com.cinetaste.recipeservice.dto.client.UserBasicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * Gọi User Service để lấy thông tin user
     * URL này trỏ đến service name trong Docker network
     */
    public UserBasicInfo getUserById(UUID userId) {
        if (userId == null) {
            System.err.println("⚠️ UserClient: userId is null");
            return createFallbackUser(userId);
        }

        try {
            // ✅ URL này phải khớp với endpoint trong User Service
            String url = "http://user-service:8081/api/users/" + userId + "/basic-info";

            System.out.println("🔍 Calling User Service: " + url);

            UserBasicInfo userInfo = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(UserBasicInfo.class)
                    .block(); // Gọi đồng bộ

            if (userInfo != null) {
                System.out.println("✅ Got user info: " + userInfo.getDisplayName());
                return userInfo;
            } else {
                System.err.println("⚠️ User Service returned null for userId: " + userId);
                return createFallbackUser(userId);
            }

        } catch (WebClientResponseException.NotFound e) {
            System.err.println("⚠️ User not found: " + userId);
            return createFallbackUser(userId);
        } catch (Exception e) {
            System.err.println("❌ Error calling User Service for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return createFallbackUser(userId);
        }
    }

    /**
     * Tạo user fallback khi không lấy được thông tin
     */
    private UserBasicInfo createFallbackUser(UUID userId) {
        UserBasicInfo fallback = new UserBasicInfo();
        fallback.setId(userId);
        fallback.setDisplayName("Unknown Chef");
        fallback.setProfileImageUrl(null);
        return fallback;
    }
}