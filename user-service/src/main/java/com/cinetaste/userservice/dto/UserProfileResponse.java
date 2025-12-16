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
    private Instant memberSince;

    private long followerCount;
    private long followingCount;

    // --- TRƯỜNG MỚI QUAN TRỌNG ---
    private boolean isFollowing;
}