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
    // Chúng ta có thể thêm các thông tin khác của tác giả (tên, ảnh đại diện) ở đây sau này
    // private String authorDisplayName;
    // private String authorProfileImageUrl;
}