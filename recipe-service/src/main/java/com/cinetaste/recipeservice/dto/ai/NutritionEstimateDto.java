package com.cinetaste.recipeservice.dto.ai;

import lombok.Data;

@Data
public class NutritionEstimateDto {
    private int calories;
    private String protein;
    private String carbs;
    private String fat;
}