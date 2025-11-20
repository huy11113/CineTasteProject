package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.Serializable;
@Data
@Builder
public class RecipeDetailResponse {
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
    private Map<String, String> nutrition; // Ví dụ: { "calories": "180", "protein": "4g" }

    // Thông tin tác giả (Lấy từ User Service sau, giờ tạm hardcode)
    private AuthorDto author;

    // Thông tin phim
    private MovieDto movie;

    // DTO con cho Tác giả
    @Data
    @Builder
    public static class AuthorDto implements Serializable {
        private UUID id;
        private String name;
        private String avatarUrl;
    }

    // DTO con cho Phim
    @Data
    @Builder
    public static class MovieDto implements Serializable {
        private String title;
        private Integer year;
        private String posterUrl;
    }
}