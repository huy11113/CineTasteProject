package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
@Data
@Builder
public class RecipeIngredientDto implements Serializable  {
    private String name;
    private String quantityUnit; // Ví dụ: "2 muỗng canh"
    private boolean isOptional;
}