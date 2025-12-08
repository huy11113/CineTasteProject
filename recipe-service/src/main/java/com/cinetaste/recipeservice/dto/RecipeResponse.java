package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

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