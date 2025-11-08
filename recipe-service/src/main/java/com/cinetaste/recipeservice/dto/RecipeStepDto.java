package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeStepDto {
    private int step;
    private String title;
    private String description;
    private String imageUrl;
}