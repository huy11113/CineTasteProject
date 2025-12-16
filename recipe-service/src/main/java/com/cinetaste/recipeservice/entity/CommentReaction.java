// ============================================================================
// 1. Entity: CommentReaction.java
// ============================================================================
package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comment_reactions")
public class CommentReaction {

    @EmbeddedId
    private CommentReactionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false, length = 10)
    private String reactionType; // 'like' hoặc 'dislike'

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}