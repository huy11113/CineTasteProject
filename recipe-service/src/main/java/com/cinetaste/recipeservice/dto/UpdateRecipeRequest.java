package com.cinetaste.recipeservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRecipeRequest {

    // Các trường này đều là tùy chọn khi cập nhật
    @Size(max = 255)
    private String title;

    private String summary;
    private Short difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Short servings;
    private String mainImageUrl;
}