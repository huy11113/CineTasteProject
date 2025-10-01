package com.cinetaste.userservice.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    // Người dùng có thể chỉ cập nhật một trong các trường này,
    // nên chúng ta không cần thêm các annotation validation như @NotBlank

    private String displayName;
    private String bio;
    private String profileImageUrl;
}