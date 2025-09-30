package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Spring Data JPA sẽ tự động tạo câu lệnh để tìm tất cả bình luận
    // của một công thức, sắp xếp theo ngày tạo gần nhất lên đầu.
    List<Comment> findByRecipeIdOrderByCreatedAtDesc(UUID recipeId);
}