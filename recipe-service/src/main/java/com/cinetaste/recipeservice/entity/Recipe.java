// Vị trí: recipe-service/src/main/java/com/cinetaste/recipeservice/entity/Recipe.java
package com.cinetaste.recipeservice.entity;

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
// (RecipeRating đã được import)

@Data
@Entity
@Table(name = "recipes")
@Where(clause = "deleted_at IS NULL")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id") // Không join, chỉ lưu ID
    private UUID authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<RecipeStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    // --- SỬA QUAN HỆ TAGS (thay vì @ManyToMany) ---
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeTag> recipeTags = new ArrayList<>();

    // --- QUAN HỆ RATINGS (Giữ nguyên) ---
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeRating> ratings = new ArrayList<>();

    // --- THÊM QUAN HỆ FAVORITES ---
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    // --- THÊM QUAN HỆ VIEWS ---
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeView> views = new ArrayList<>();

    // --- THÊM QUAN HỆ REPORTS ---
    @OneToMany(mappedBy = "reportedRecipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();

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

    // --- THÊM CÁC TRƯỜNG MỚI ---
    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "published_at")
    private Instant publishedAt;
    // --- KẾT THÚC THÊM ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
}