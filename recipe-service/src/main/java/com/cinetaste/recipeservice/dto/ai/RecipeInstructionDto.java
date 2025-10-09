package com.cinetaste.recipeservice.dto.ai;

import lombok.Data;

@Data
public class RecipeInstructionDto {
    private int step;
    private String description;
}