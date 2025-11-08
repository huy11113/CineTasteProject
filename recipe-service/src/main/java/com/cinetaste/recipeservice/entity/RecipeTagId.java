package com.cinetaste.recipeservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class RecipeTagId implements Serializable {
    private UUID recipeId;
    private Integer tagId;
}