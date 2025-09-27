// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/dto/RateRecipeRequest.java
package com.cinetaste.recipeservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateRecipeRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Short rating;
    private String review;
}