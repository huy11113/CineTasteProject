package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeIngredientDto {
    private String name;
    private String quantityUnit; // Ví dụ: "2 muỗng canh"
    private boolean isOptional;
}