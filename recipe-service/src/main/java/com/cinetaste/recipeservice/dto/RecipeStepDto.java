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
public class RecipeStepDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private int step;
    private String title;
    private String description;
    private String imageUrl;
}