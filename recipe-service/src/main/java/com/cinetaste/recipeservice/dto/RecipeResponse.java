// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/dto/RecipeResponse.java
package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RecipeResponse {
    private UUID id;
    private UUID authorId;
    private String title;
    private String slug;
    private String summary;
    private Short difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Short servings;
    private String mainImageUrl;
    private BigDecimal avgRating;
    private Instant createdAt;
    private String movieTitle;
    private Integer ratingsCount;
}