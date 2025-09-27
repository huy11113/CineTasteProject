// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/entity/RecipeRatingId.java
package com.cinetaste.recipeservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable // Đánh dấu đây là một lớp có thể được nhúng vào một Entity khác
public class RecipeRatingId implements Serializable {

    private UUID userId;
    private UUID recipeId;

    // Cần có constructor không tham số và các hàm equals(), hashCode()
    // @Data của Lombok đã tự làm việc này cho chúng ta
}