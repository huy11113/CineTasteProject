package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Đổi tên hàm này để lọc ra các comment chưa bị xóa mềm
    List<Comment> findByRecipeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID recipeId);

    // HÀM MỚI: Đếm số lượng comment
    long countByRecipeIdAndDeletedAtIsNull(UUID recipeId);
}