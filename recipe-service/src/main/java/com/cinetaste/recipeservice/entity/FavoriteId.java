package com.cinetaste.recipeservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class FavoriteId implements Serializable {
    private UUID userId;
    private UUID recipeId;
}