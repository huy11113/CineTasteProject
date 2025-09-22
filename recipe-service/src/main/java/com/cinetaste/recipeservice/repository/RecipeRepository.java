package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    // Spring sẽ tự tạo các hàm CRUD cơ bản
}
