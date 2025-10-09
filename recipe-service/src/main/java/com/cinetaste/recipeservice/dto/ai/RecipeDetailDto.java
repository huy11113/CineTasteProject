package com.cinetaste.recipeservice.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class RecipeDetailDto {
    private int difficulty;
    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private int servings;
    private List<RecipeIngredientDto> ingredients;
    private List<RecipeInstructionDto> instructions;
}