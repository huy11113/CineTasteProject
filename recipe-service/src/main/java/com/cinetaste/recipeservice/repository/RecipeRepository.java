package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    // --- THÊM DÒNG NÀY ---
    Page<Recipe> findByAuthorId(UUID authorId, Pageable pageable);
}