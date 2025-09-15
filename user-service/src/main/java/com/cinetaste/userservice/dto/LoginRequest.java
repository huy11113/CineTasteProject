package com.cinetaste.userservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username or Email is required")
    private String loginIdentifier; // <-- Đổi tên từ username

    @NotBlank(message = "Password is required")
    private String password;
}
