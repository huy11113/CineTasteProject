package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.*;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.entity.*;
import com.cinetaste.recipeservice.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cinetaste.recipeservice.repository.CommentRepository;
import org.springframework.context.annotation.Lazy;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant; // <-- THÊM IMPORT ĐÃ THIẾU (Sửa Lỗi 4)
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeRatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final AiRequestsLogRepository logRepository;
    private final @Lazy AiClientService aiClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // (Hàm createRecipe không đổi)
    @Transactional
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
        // TODO: Cần thêm logic để lưu steps và ingredients từ request
        return recipeRepository.save(newRecipe);
    }

    // (Hàm generateSlug không đổi)
    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH) + "-" + System.currentTimeMillis();
    }

    // (Hàm getAllRecipes không đổi)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable)
                .map(this::mapToRecipeResponse);
    }

    // (Hàm mapToRecipeResponse tóm tắt - giữ nguyên)
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
                .movieTitle(recipe.getMovie() != null ? recipe.getMovie().getTitle() : null) // <-- Sửa lỗi NullPointer
                .build();
    }

    // --- HÀM MỚI (trả về chi tiết) ---
    public RecipeDetailResponse getRecipeDetailById(UUID recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));

        // TODO: Trong tương lai, bạn sẽ gọi User-Service bằng WebClient để lấy thông tin tác giả thật
        // Tạm thời hardcode
        RecipeDetailResponse.AuthorDto author = RecipeDetailResponse.AuthorDto.builder()
                .id(recipe.getAuthorId())
                .name("Chef Auguste (Tạm)") // Tên hardcode
                .avatarUrl("https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg?auto=compress&cs=tinysrgb&w=200") // Ảnh hardcode
                .build();

        // Lấy thông tin phim
        RecipeDetailResponse.MovieDto movie = null;
        if (recipe.getMovie() != null) {
            movie = RecipeDetailResponse.MovieDto.builder()
                    .title(recipe.getMovie().getTitle())
                    // Sửa lỗi 2: Dùng getter của Lombok
                    .year(recipe.getMovie().getReleaseDate() != null ? recipe.getMovie().getReleaseDate().getYear() : null)
                    .posterUrl(recipe.getMovie().getPosterUrl())
                    .build();
        }

        return mapToRecipeDetailResponse(recipe, author, movie);
    }

    // --- HÀM HELPER MỚI: Map sang DTO chi tiết ---
    private RecipeDetailResponse mapToRecipeDetailResponse(Recipe recipe, RecipeDetailResponse.AuthorDto author, RecipeDetailResponse.MovieDto movie) {

        // Chuyển đổi danh sách Ingredients
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

        // Chuyển đổi danh sách Steps
        List<RecipeStepDto> stepDtos = recipe.getSteps().stream()
                .map(step -> RecipeStepDto.builder()
                        .step(step.getStepOrder())
                        .title("Bước " + step.getStepOrder()) // CSDL mới chưa có title, ta tự tạo
                        .description(step.getInstructions())
                        .imageUrl(step.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        // Chuyển đổi Nutrition Info (JSON string sang Map)
        Map<String, String> nutritionMap = Collections.emptyMap();
        if (recipe.getNutritionInfo() != null && !recipe.getNutritionInfo().isEmpty()) {
            try {
                nutritionMap = objectMapper.readValue(recipe.getNutritionInfo(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                // Bỏ qua nếu JSON lỗi
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
        // Tạm thời gọi hàm này, mặc dù CSDL đã có trigger
        updateRecipeAverageRating(recipe);
    }

    // Cần hàm này để lấy List<RecipeRating>
    private void updateRecipeAverageRating(Recipe recipe) {
        // Phải gọi qua repo để lấy list ratings mới nhất
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

    // Cần hàm này để lấy count
    private void updateRecipeCommentsCount(Recipe recipe) {
        long count = commentRepository.countByRecipeIdAndDeletedAtIsNull(recipe.getId());
        recipe.setCommentsCount((int) count);
        recipeRepository.save(recipe);
    }

    public List<CommentResponse> getComments(UUID recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new RuntimeException("Recipe not found with id: " + recipeId);
        }
        // Đảm bảo chỉ lấy comment chưa bị xóa
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

    @Transactional
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
    public void deleteRecipe(UUID recipeId, UUID currentUserId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));
        if (!recipe.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this recipe.");
        }
        // Sửa lỗi 4: Dùng Instant.now()
        recipe.setDeletedAt(Instant.now());
        recipeRepository.save(recipe);
    }

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
}