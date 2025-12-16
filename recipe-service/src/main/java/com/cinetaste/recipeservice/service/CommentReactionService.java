package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.CommentReactionDto;
import com.cinetaste.recipeservice.entity.Comment;
import com.cinetaste.recipeservice.entity.CommentReaction;
import com.cinetaste.recipeservice.entity.CommentReactionId;
import com.cinetaste.recipeservice.repository.CommentReactionRepository;
import com.cinetaste.recipeservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentReactionService {

    private final CommentReactionRepository reactionRepository;
    private final CommentRepository commentRepository;

    /**
     * React to comment - LOGIC ĐÚNG
     * <p>
     * Logic:
     * 1. Nếu user chưa react → Tạo reaction mới
     * 2. Nếu user đã react loại giống nhau → XÓA reaction (toggle off)
     * 3. Nếu user đã react loại khác → CẬP NHẬT reaction (chuyển loại)
     */
    @Transactional
    public CommentReactionDto reactToComment(Long commentId, UUID userId, String reactionType) {
        // Validate comment exists
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        // Kiểm tra reaction hiện tại của user
        CommentReactionId reactionId = new CommentReactionId(userId, commentId);
        var existingReaction = reactionRepository.findById(reactionId);

        String userCurrentReaction = null;

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();

            // Case 1: Nhấn cùng loại reaction → Xóa (toggle off)
            if (reaction.getReactionType().equals(reactionType)) {
                reactionRepository.delete(reaction);
                userCurrentReaction = null; // User đã hủy reaction
            }
            // Case 2: Nhấn loại reaction khác → Cập nhật
            else {
                reaction.setReactionType(reactionType);
                reactionRepository.save(reaction);
                userCurrentReaction = reactionType;
            }
        } else {
            // Case 3: Chưa có reaction → Tạo mới
            CommentReaction newReaction = new CommentReaction();
            newReaction.setId(reactionId);
            newReaction.setComment(comment);
            newReaction.setReactionType(reactionType);
            reactionRepository.save(newReaction);
            userCurrentReaction = reactionType;
        }

        // Lấy counts mới từ database
        long likes = reactionRepository.countByCommentIdAndReactionType(commentId, "like");
        long dislikes = reactionRepository.countByCommentIdAndReactionType(commentId, "dislike");

        return CommentReactionDto.builder()
                .reactionType(reactionType)
                .isActive(userCurrentReaction != null && userCurrentReaction.equals(reactionType))
                .likeCount(likes)
                .dislikeCount(dislikes)
                .userCurrentReaction(userCurrentReaction)
                .build();
    }
}