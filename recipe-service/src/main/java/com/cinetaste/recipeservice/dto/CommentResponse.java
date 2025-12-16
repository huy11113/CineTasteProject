// ============================================================================
// 4. DTO: CommentResponse.java (CẬP NHẬT)
// ============================================================================
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

    // Thông tin tác giả
    private String authorDisplayName;
    private String authorProfileImageUrl;

    // ===== PHẦN MỚI: REACTIONS =====
    private long likes;          // Số lượng likes
    private long dislikes;       // Số lượng dislikes
    private String userReaction; // 'like', 'dislike', hoặc null
}