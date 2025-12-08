package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class RecipeDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // Thông tin cơ bản (giống RecipeResponse)
    private UUID id;
    private String title;
    private String slug;
    private String summary;
    private Short difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Short servings;
    private String mainImageUrl;
    private BigDecimal avgRating;
    private Integer ratingsCount;
    private Instant createdAt;

    // Thông tin chi tiết
    private List<RecipeIngredientDto> ingredients;
    private List<RecipeStepDto> instructions;
    private Map<String, String> nutrition;

    // Thông tin tác giả
    private AuthorDto author;

    // Thông tin phim
    private MovieDto movie;

    // DTO con cho Tác giả
    @Data
    @Builder
    public static class AuthorDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private UUID id;
        private String name;
        private String avatarUrl;
    }

    // DTO con cho Phim
    @Data
    @Builder
    public static class MovieDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private Integer year;
        private String posterUrl;
    }
}