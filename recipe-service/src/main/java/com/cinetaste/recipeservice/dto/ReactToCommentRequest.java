// ============================================================================
// 5. DTO: ReactToCommentRequest.java (MỚI)
// ============================================================================
package com.cinetaste.recipeservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReactToCommentRequest {
    @NotNull
    @Pattern(regexp = "^(like|dislike)$", message = "Reaction must be 'like' or 'dislike'")
    private String reactionType;
}