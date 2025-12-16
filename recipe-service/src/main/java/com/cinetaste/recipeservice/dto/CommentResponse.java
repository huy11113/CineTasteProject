package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private UUID authorId;
    private Long parentId;
    private String content;
    private Instant createdAt;

    // --- THÊM TRƯỜNG ---
    private String authorDisplayName;
    private String authorProfileImageUrl;
}