package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.RecipeRating;
import com.cinetaste.recipeservice.entity.RecipeRatingId; // <-- THÊM IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <-- THÊM IMPORT
import java.util.UUID;

@Repository
public interface RecipeRatingRepository extends JpaRepository<RecipeRating, RecipeRatingId> { // <-- SỬA ID

    // HÀM MỚI: Tìm tất cả rating của một recipe
    List<RecipeRating> findByRecipeId(UUID recipeId);
}