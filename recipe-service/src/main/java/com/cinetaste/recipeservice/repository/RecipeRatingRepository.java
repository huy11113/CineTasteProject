// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/repository/RecipeRatingRepository.java
package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.RecipeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeRatingRepository extends JpaRepository<RecipeRating, Long> {
    // Chúng ta có thể thêm các hàm tùy chỉnh ở đây sau này
}