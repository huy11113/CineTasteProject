package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.client.TmdbClient;
import com.cinetaste.recipeservice.client.UserClient;
import com.cinetaste.recipeservice.dto.*;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.dto.client.UserBasicInfo;
import com.cinetaste.recipeservice.dto.tmdb.TmdbMovieResult;
import com.cinetaste.recipeservice.entity.*;
import com.cinetaste.recipeservice.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    // --- 1. REPOSITORIES ---
    private final RecipeRepository recipeRepository;
    private final RecipeRatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final AiRequestsLogRepository logRepository;
    private final MovieRepository movieRepository; // Mới: Cho TMDB

    // --- 2. EXTERNAL CLIENTS ---
    @Lazy
    private final AiClientService aiClientService;
    private final UserClient userClient;   // Mới: Lấy info user thật
    private final TmdbClient tmdbClient;   // Mới: Lấy info phim thật

    // --- 3. UTILS & CONFIG ---
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tmdb.image.base-url}")
    private String imageBaseUrl;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // =========================================================================
    // 1. CREATE (Tạo mới) - Có tích hợp TMDB & Xóa Cache
    // =========================================================================
    @Transactional
    @CacheEvict(value = "recipes", allEntries = true) // Xóa cache danh sách khi có bài mới
    public Recipe createRecipe(CreateRecipeRequest request, UUID authorId) {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle(request.getTitle());
        newRecipe.setSummary(request.getSummary());
        newRecipe.setDifficulty(request.getDifficulty());
        newRecipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        newRecipe.setCookTimeMinutes(request.getCookTimeMinutes());
        newRecipe.setServings(request.getServings());
        newRecipe.setMainImageUrl(request.getMainImageUrl());
        newRecipe.setAuthorId(authorId);
        newRecipe.setSlug(generateSlug(request.getTitle()));

        // --- LOGIC TÌM PHIM TỪ TMDB ---
        if (request.getMovieName() != null && !request.getMovieName().isEmpty()) {
            TmdbMovieResult tmdbResult = tmdbClient.searchMovie(request.getMovieName());

            if (tmdbResult != null) {
                // Tìm trong DB xem có chưa, chưa có thì tạo mới
                Movie movie = movieRepository.findByTmdbId(tmdbResult.getId())
                        .orElseGet(() -> {
                            Movie newMovie = new Movie();
                            newMovie.setTmdbId(tmdbResult.getId());
                            newMovie.setTitle(tmdbResult.getTitle());
                            newMovie.setOverview(tmdbResult.getOverview());

                            if (tmdbResult.getPosterPath() != null) {
                                newMovie.setPosterUrl(imageBaseUrl + tmdbResult.getPosterPath());
                            }
                            if (tmdbResult.getBackdropPath() != null) {
                                newMovie.setBackdropUrl(imageBaseUrl + tmdbResult.getBackdropPath());
                            }

                            newMovie.setReleaseDate(tmdbResult.getReleaseDate());
                            if (tmdbResult.getVoteAverage() != null) {
                                newMovie.setVoteAverage(BigDecimal.valueOf(tmdbResult.getVoteAverage()));
                            }
                            newMovie.setVoteCount(tmdbResult.getVoteCount());

                            return movieRepository.save(newMovie);
                        });

                newRecipe.setMovie(movie);
            }
        }
        // ------------------------------

        return recipeRepository.save(newRecipe);
    }

    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH) + "-" + System.currentTimeMillis();
    }

    // =========================================================================
    // 2. READ (Đọc) - Có Caching Redis & Gọi User Service
    // =========================================================================

    @Cacheable(value = "recipes", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable)
                .map(this::mapToRecipeResponse);
    }

    private RecipeResponse mapToRecipeResponse(Recipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .authorId(recipe.getAuthorId())
                .title(recipe.getTitle())
                .slug(recipe.getSlug())
                .summary(recipe.getSummary())
                .difficulty(recipe.getDifficulty())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .servings(recipe.getServings())
                .mainImageUrl(recipe.getMainImageUrl())
                .avgRating(recipe.getAvgRating())
                .createdAt(recipe.getCreatedAt())
                .ratingsCount(recipe.getRatingsCount())
                .movieTitle(recipe.getMovie() != null ? recipe.getMovie().getTitle() : null)
                .build();
    }

    @Cacheable(value = "recipe_details", key = "#recipeId")
    public RecipeDetailResponse getRecipeDetailById(UUID recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));

        // --- GỌI USER SERVICE LẤY INFO TÁC GIẢ ---
        UserBasicInfo authorInfo = userClient.getUserById(recipe.getAuthorId());

        RecipeDetailResponse.AuthorDto author = RecipeDetailResponse.AuthorDto.builder()
                .id(recipe.getAuthorId())
                .name(authorInfo != null ? authorInfo.getDisplayName() : "Unknown Chef")
                .avatarUrl(authorInfo != null ? authorInfo.getProfileImageUrl() : null)
                .build();
        // -----------------------------------------

        RecipeDetailResponse.MovieDto movie = null;
        if (recipe.getMovie() != null) {
            movie = RecipeDetailResponse.MovieDto.builder()
                    .title(recipe.getMovie().getTitle())
                    .year(recipe.getMovie().getReleaseDate() != null ? recipe.getMovie().getReleaseDate().getYear() : null)
                    .posterUrl(recipe.getMovie().getPosterUrl())
                    .build();
        }

        return mapToRecipeDetailResponse(recipe, author, movie);
    }

    private RecipeDetailResponse mapToRecipeDetailResponse(Recipe recipe, RecipeDetailResponse.AuthorDto author, RecipeDetailResponse.MovieDto movie) {
        List<RecipeIngredientDto> ingredientDtos = recipe.getIngredients().stream()
                .map(ing -> {
                    String quantity = (ing.getQuantity() != null) ? ing.getQuantity().stripTrailingZeros().toPlainString() : "";
                    String unit = (ing.getUnit() != null) ? ing.getUnit() : "";
                    return RecipeIngredientDto.builder()
                            .name(ing.getIngredient().getName())
                            .quantityUnit(String.format("%s %s", quantity, unit).trim())
                            .isOptional(ing.getIsOptional() != null && ing.getIsOptional())
                            .build();
                })
                .collect(Collectors.toList());

        List<RecipeStepDto> stepDtos = recipe.getSteps().stream()
                .map(step -> RecipeStepDto.builder()
                        .step(step.getStepOrder())
                        .title("Bước " + step.getStepOrder())
                        .description(step.getInstructions())
                        .imageUrl(step.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        Map<String, String> nutritionMap = Collections.emptyMap();
        if (recipe.getNutritionInfo() != null && !recipe.getNutritionInfo().isEmpty()) {
            try {
                nutritionMap = objectMapper.readValue(recipe.getNutritionInfo(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                // Ignore error
            }
        }

        return RecipeDetailResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .slug(recipe.getSlug())
                .summary(recipe.getSummary())
                .difficulty(recipe.getDifficulty())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .servings(recipe.getServings())
                .mainImageUrl(recipe.getMainImageUrl())
                .avgRating(recipe.getAvgRating())
                .ratingsCount(recipe.getRatingsCount())
                .createdAt(recipe.getCreatedAt())
                .ingredients(ingredientDtos)
                .instructions(stepDtos)
                .nutrition(nutritionMap)
                .author(author)
                .movie(movie)
                .build();
    }

    // =========================================================================
    // 3. UPDATE & DELETE - Xóa Cache liên quan
    // =========================================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "recipes", allEntries = true),
            @CacheEvict(value = "recipe_details", key = "#recipeId")
    })
    public RecipeResponse updateRecipe(UUID recipeId, UUID currentUserId, UpdateRecipeRequest request) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));
        if (!recipe.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to edit this recipe.");
        }
        Optional.ofNullable(request.getTitle()).ifPresent(recipe::setTitle);
        Optional.ofNullable(request.getSummary()).ifPresent(recipe::setSummary);
        Optional.ofNullable(request.getDifficulty()).ifPresent(recipe::setDifficulty);
        Optional.ofNullable(request.getPrepTimeMinutes()).ifPresent(recipe::setPrepTimeMinutes);
        Optional.ofNullable(request.getCookTimeMinutes()).ifPresent(recipe::setCookTimeMinutes);
        Optional.ofNullable(request.getServings()).ifPresent(recipe::setServings);
        Optional.ofNullable(request.getMainImageUrl()).ifPresent(recipe::setMainImageUrl);

        Recipe updatedRecipe = recipeRepository.save(recipe);
        return mapToRecipeResponse(updatedRecipe);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "recipes", allEntries = true),
            @CacheEvict(value = "recipe_details", key = "#recipeId")
    })
    public void deleteRecipe(UUID recipeId, UUID currentUserId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));
        if (!recipe.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this recipe.");
        }
        recipe.setDeletedAt(Instant.now());
        recipeRepository.save(recipe);
    }

    // =========================================================================
    // 4. INTERACTION (Rating, Comment)
    // =========================================================================

    @Transactional
    public void rateRecipe(UUID recipeId, RateRecipeRequest request, UUID userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        RecipeRatingId ratingId = new RecipeRatingId();
        ratingId.setUserId(userId);
        ratingId.setRecipeId(recipeId);

        RecipeRating rating = ratingRepository.findById(ratingId).orElse(new RecipeRating());

        rating.setId(ratingId);
        rating.setRecipe(recipe);
        rating.setRating(request.getRating());
        rating.setReview(request.getReview());

        ratingRepository.save(rating);
        updateRecipeAverageRating(recipe);
    }

    private void updateRecipeAverageRating(Recipe recipe) {
        List<RecipeRating> ratings = ratingRepository.findByRecipeId(recipe.getId());
        if (ratings == null || ratings.isEmpty()) {
            recipe.setAvgRating(BigDecimal.ZERO);
            recipe.setRatingsCount(0);
        } else {
            double average = ratings.stream()
                    .mapToInt(RecipeRating::getRating)
                    .average()
                    .orElse(0.0);
            recipe.setAvgRating(BigDecimal.valueOf(average));
            recipe.setRatingsCount(ratings.size());
        }
        recipeRepository.save(recipe);
    }

    @Transactional
    public CommentResponse addComment(UUID recipeId, UUID authorId, CreateCommentRequest request) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));
        Comment newComment = new Comment();
        newComment.setRecipe(recipe);
        newComment.setAuthorId(authorId);
        newComment.setContent(request.getContent());
        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            newComment.setParent(parentComment);
        }
        Comment savedComment = commentRepository.save(newComment);

        updateRecipeCommentsCount(recipe);

        return mapToCommentResponse(savedComment);
    }

    private void updateRecipeCommentsCount(Recipe recipe) {
        long count = commentRepository.countByRecipeIdAndDeletedAtIsNull(recipe.getId());
        recipe.setCommentsCount((int) count);
        recipeRepository.save(recipe);
    }

    public List<CommentResponse> getComments(UUID recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new RuntimeException("Recipe not found with id: " + recipeId);
        }
        return commentRepository.findByRecipeIdAndDeletedAtIsNullOrderByCreatedAtDesc(recipeId)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // =========================================================================
    // 5. AI ANALYSIS
    // =========================================================================

    public Mono<AnalyzeDishResponse> analyzeDishAndLog(MultipartFile image, String context, UUID userId) {
        long startTime = System.currentTimeMillis();
        return aiClientService.analyzeDish(image, context)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String responseSummary = (response.getDescription() != null && response.getDescription().length() > 200) ?
                            response.getDescription().substring(0, 200) : response.getDescription();
                    logRequest(userId, "analyze-dish", "Image + context", responseSummary, duration, true, null);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logRequest(userId, "analyze-dish", "Image + context", null, duration, false, error.getMessage());
                });
    }

    private void logRequest(UUID userId, String feature, String requestSummary, String responseSummary, long duration, boolean success, String errorMessage) {
        AiRequestsLog log = AiRequestsLog.builder()
                .userId(userId)
                .feature(feature)
                .requestPayloadSummary(requestSummary)
                .responsePayloadSummary(responseSummary)
                .durationMs((int) duration)
                .success(success)
                .errorMessage(errorMessage)
                .build();
        logRepository.save(log);
    }
    // --- THÊM HÀM MỚI ---
    public Page<RecipeResponse> getRecipesByAuthorId(UUID authorId, Pageable pageable) {
        return recipeRepository.findByAuthorId(authorId, pageable)
                .map(this::mapToRecipeResponse);
    }

}