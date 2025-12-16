// ============================================================================
// File: recipe-service/src/main/java/com/cinetaste/recipeservice/dto/CommentReactionDto.java
// ============================================================================

package com.cinetaste.recipeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionDto {

    private String reactionType;           // 'like' hoặc 'dislike'

    private boolean isActive;              // true = user đang like/dislike cái này

    private long likeCount;                // Số lượng likes

    private long dislikeCount;             // Số lượng dislikes

    private String userCurrentReaction;    // 'like' | 'dislike' | null
    // null = user không react hoặc đã hủy
}