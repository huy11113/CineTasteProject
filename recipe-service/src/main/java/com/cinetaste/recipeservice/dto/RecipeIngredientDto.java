package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String quantityUnit; // Ví dụ: "2 muỗng canh"
    private boolean isOptional;
}