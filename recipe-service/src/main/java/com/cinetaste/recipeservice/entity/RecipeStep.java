package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "recipe_steps")
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "step_order", nullable = false)
    private Short stepOrder;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    @Column(name = "image_url")
    private String imageUrl;
}