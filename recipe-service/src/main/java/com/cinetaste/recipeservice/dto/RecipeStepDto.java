package com.cinetaste.recipeservice.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
@Data
@Builder
public class RecipeStepDto implements Serializable {
    private int step;
    private String title;
    private String description;
    private String imageUrl;
}