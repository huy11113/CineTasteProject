// ============================================================================
// 3. Repository: CommentReactionRepository.java
// ============================================================================
package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.CommentReaction;
import com.cinetaste.recipeservice.entity.CommentReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, CommentReactionId> {

    // Đếm số likes của một comment
    long countByCommentIdAndReactionType(Long commentId, String reactionType);

    // Lấy reaction của user cho một comment cụ thể
    @Query("SELECT r.reactionType FROM CommentReaction r WHERE r.id.userId = :userId AND r.id.commentId = :commentId")
    String findReactionTypeByUserIdAndCommentId(UUID userId, Long commentId);
}