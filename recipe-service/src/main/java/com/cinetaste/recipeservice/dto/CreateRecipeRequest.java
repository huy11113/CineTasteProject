package com.cinetaste.recipeservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRecipeRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255)
    private String title;

    private String summary;

    @NotBlank(message = "Instructions are required")
    private String instructions;

    private Short difficulty;

    private Integer prepTimeMinutes;

    private Integer cookTimeMinutes;

    private Short servings;

    private String mainImageUrl;

    private String movieName;
}
