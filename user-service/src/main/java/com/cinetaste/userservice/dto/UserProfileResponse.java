package com.cinetaste.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private Instant memberSince; // createdAt

    // Thống kê xã hội
    private long followerCount;
    private long followingCount;
}