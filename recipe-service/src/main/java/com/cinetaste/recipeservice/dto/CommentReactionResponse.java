package com.cinetaste.recipeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CommentReactionResponse {
    private Long commentId;
    private long likes;           // Tổng số likes
    private long dislikes;        // Tổng số dislikes
    private String userReaction;  // 'like', 'dislike', hoặc null của user hiện tại
}