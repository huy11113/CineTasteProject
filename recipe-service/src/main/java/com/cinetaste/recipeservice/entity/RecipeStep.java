package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "recipe_steps")
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "step_order", nullable = false)
    private Short stepOrder;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    @Column(name = "image_url")
    private String imageUrl;

    // --- THÊM TRƯỜNG MỚI ---
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    // --- KẾT THÚC THÊM ---
}