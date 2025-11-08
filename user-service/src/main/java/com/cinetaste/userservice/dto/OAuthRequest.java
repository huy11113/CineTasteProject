package com.cinetaste.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthRequest {
    @NotBlank(message = "Token is required")
    private String token; // Đây sẽ là Access Token từ Google
}