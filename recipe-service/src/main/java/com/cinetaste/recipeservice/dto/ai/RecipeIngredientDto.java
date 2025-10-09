package com.cinetaste.recipeservice.dto.ai;

import lombok.Data;

@Data
public class RecipeIngredientDto {
    private String name;
    private String quantity;
    private String unit;
}