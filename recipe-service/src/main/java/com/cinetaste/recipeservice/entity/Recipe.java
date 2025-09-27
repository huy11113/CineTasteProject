// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/entity/Recipe.java
package com.cinetaste.recipeservice.entity;

// Đảm bảo các import này là chính xác
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Data
@Entity
@Table(name = "recipes")
@Where(clause = "deleted_at IS NULL") // Dành cho soft-delete
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    // --- Mối quan hệ với Movie ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // --- Mối quan hệ với RecipeStep ---
    @OneToMany(
            mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stepOrder ASC")
    private List<RecipeStep> steps = new ArrayList<>();

    // --- Mối quan hệ với RecipeIngredient ---
    @OneToMany(
            mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    // --- Mối quan hệ với Tag ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "recipe_tags",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    private Short difficulty;
    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;
    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;
    private Short servings;
    @Column(nullable = false)
    private String status = "draft";
    @Column(nullable = false)
    private String visibility = "public";
    @Column(name = "main_image_url")
    private String mainImageUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nutrition_info", columnDefinition = "jsonb")
    private String nutritionInfo;
    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;
    @Column(name = "ratings_count", nullable = false)
    private Integer ratingsCount = 0;
    @Column(name = "comments_count", nullable = false)
    private Integer commentsCount = 0;
    @Column(name = "favorites_count", nullable = false)
    private Integer favoritesCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
}