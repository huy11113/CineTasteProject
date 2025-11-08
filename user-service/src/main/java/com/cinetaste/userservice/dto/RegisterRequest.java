package com.cinetaste.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size; // <-- THÊM IMPORT NÀY
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    // THÊM DÒNG NÀY ĐỂ KHỚP VỚI DB VÀ FRONTEND
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters, contain 1 uppercase, 1 lowercase, 1 number, and 1 special character."
    )
    private String password;
}