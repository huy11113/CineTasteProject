package com.cinetaste.recipeservice.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class AnalyzeDishResponse {
    @JsonProperty("dish_name")
    private String dishName;
    private String origin;
    private String description;
    @JsonProperty("nutrition_estimate")
    private NutritionEstimateDto nutritionEstimate;
    @JsonProperty("health_tags")
    private List<String> healthTags;
    @JsonProperty("pairing_suggestions")
    private PairingSuggestionsDto pairingSuggestions;
    private RecipeDetailDto recipe;
    private List<String> tips;
}