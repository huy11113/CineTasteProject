package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Data
@Entity
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    // --- THÊM CÁC TRƯỜNG MỚI ---
    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "is_allergen")
    private Boolean isAllergen = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    // --- KẾT THÚC THÊM ---
}