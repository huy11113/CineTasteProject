package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// --- SỬA LỖI DEPRECATED ---
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Filter;
// --- KẾT THÚC SỬA ---
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
// --- SỬA LỖI DEPRECATED: Dùng cách soft-delete mới ---
@SQLDelete(sql = "UPDATE recipes SET deleted_at = NOW() WHERE id = ?")
@FilterDef(name = "deletedRecipeFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "deletedRecipeFilter", condition = "deleted_at IS NULL")
// --- KẾT THÚC SỬA ---
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id")
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

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeTag> recipeTags = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeRating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeView> views = new ArrayList<>();

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

    // --- TRƯỜNG MỚI (KHỚP VỚI CSDL) ---
    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "published_at")
    private Instant publishedAt;
    // --- KẾT THÚC TRƯỜNG MỚI ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
}