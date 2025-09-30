package com.cinetaste.recipeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment content cannot be blank")
    private String content;

    // ID của bình luận cha (nếu đây là một bình luận trả lời)
    private Long parentId;
}