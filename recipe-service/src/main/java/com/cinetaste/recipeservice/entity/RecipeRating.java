// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/entity/RecipeRating.java
package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "recipe_ratings")
public class RecipeRating {

    // Sử dụng @EmbeddedId thay vì @Id
    @EmbeddedId
    private RecipeRatingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("recipeId") // Ánh xạ thuộc tính recipeId của lớp Id
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    // Chúng ta không cần thuộc tính userId ở đây nữa vì nó đã nằm trong id.

    @Column(nullable = false)
    private Short rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}