// ============================================================================
// 2. Embeddable ID: CommentReactionId.java
// ============================================================================
package com.cinetaste.recipeservice.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CommentReactionId implements Serializable {
    private UUID userId;
    private Long commentId;
}