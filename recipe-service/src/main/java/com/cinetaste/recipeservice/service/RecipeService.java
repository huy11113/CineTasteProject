package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.CommentResponse;
import com.cinetaste.recipeservice.dto.CreateRecipeRequest;
import com.cinetaste.recipeservice.dto.RateRecipeRequest;
import com.cinetaste.recipeservice.dto.RecipeResponse;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.entity.*;
import com.cinetaste.recipeservice.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cinetaste.recipeservice.entity.RecipeRating;
import com.cinetaste.recipeservice.dto.CommentResponse;
import com.cinetaste.recipeservice.dto.CreateCommentRequest;
import com.cinetaste.recipeservice.repository.CommentRepository;
import org.springframework.context.annotation.Lazy;
import com.cinetaste.recipeservice.dto.UpdateRecipeRequest;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public Recipe createRecipe(CreateRecipeRequest request, UUID authorId) {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle(request.getTitle());
        newRecipe.setSummary(request.getSummary());
        newRecipe.setDifficulty(request.getDifficulty());
        newRecipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        newRecipe.setCookTimeMinutes(request.getCookTimeMinutes());
        newRecipe.setServings(request.getServings());
        newRecipe.setMainImageUrl(request.getMainImageUrl());
        // newRecipe.setMovieName(request.getMovieName()); // Dữ liệu này sẽ được quản lý qua bảng movie

        // Gán ID của tác giả
        newRecipe.setAuthorId(authorId);

        // Tạo một slug duy nhất từ tiêu đề
        newRecipe.setSlug(generateSlug(request.getTitle()));

        return recipeRepository.save(newRecipe);
    }

    // Hàm tiện ích để tạo slug (ví dụ: "Món Ăn Ngon" -> "mon-an-ngon")
    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH) + "-" + System.currentTimeMillis(); // Thêm timestamp để đảm bảo duy nhất
    }
    // --- HÀM MỚI ---
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable) // Truyền pageable vào repository
                .map(this::mapToRecipeResponse); // map kết quả Page<Recipe> thành Page<RecipeResponse>
    }

    // --- HÀM MỚI ---
    public RecipeResponse getRecipeById(UUID recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));
        return mapToRecipeResponse(recipe);
    }

    // --- HÀM TIỆN ÍCH MỚI ---
    // Chuyển đổi từ Entity sang DTO để trả về cho client
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
                .build();
    }

    @Transactional
    public void rateRecipe(UUID recipeId, RateRecipeRequest request, UUID userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Tạo đối tượng khóa chính kết hợp
        RecipeRatingId ratingId = new RecipeRatingId();
        ratingId.setUserId(userId);
        ratingId.setRecipeId(recipeId);

        RecipeRating newRating = new RecipeRating();
        newRating.setId(ratingId); // Gán khóa chính kết hợp
        newRating.setRecipe(recipe);
        newRating.setRating(request.getRating());
        newRating.setReview(request.getReview()); // Giả sử DTO có thêm trường review

        ratingRepository.save(newRating);

        // Cập nhật lại điểm trung bình cho công thức
        updateRecipeAverageRating(recipe);
    }
    // --- HÀM TIỆN ÍCH MỚI ---
    private void updateRecipeAverageRating(Recipe recipe) {
        // Đây là cách tính đơn giản, có thể tối ưu sau này
        List<RecipeRating> ratings = recipe.getRatings(); // Giả sử Recipe entity có List<RecipeRating>
        if (ratings == null || ratings.isEmpty()) {
            return;
        }

        double average = ratings.stream()
                .mapToInt(RecipeRating::getRating)
                .average()
                .orElse(0.0);

        recipe.setAvgRating(BigDecimal.valueOf(average));
        recipe.setRatingsCount(ratings.size());
        recipeRepository.save(recipe);
    }
    // --- Thêm bình luận ---
    public CommentResponse addComment(UUID recipeId, UUID authorId, CreateCommentRequest request) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));

        Comment newComment = new Comment();
        newComment.setRecipe(recipe);
        newComment.setAuthorId(authorId);
        newComment.setContent(request.getContent());

        // Xử lý bình luận trả lời
        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            newComment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(newComment);
        return mapToCommentResponse(savedComment);
    }

    // --- HÀM MỚI: Lấy danh sách bình luận ---
    public List<CommentResponse> getComments(UUID recipeId) {
        // Kiểm tra xem công thức có tồn tại không
        if (!recipeRepository.existsById(recipeId)) {
            throw new RuntimeException("Recipe not found with id: " + recipeId);
        }
        return commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    // --- HÀM TIỆN ÍCH MỚI: Chuyển đổi Comment Entity sang DTO ---
    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
    // --- PHƯƠNG THỨC MỚI: Cập nhật công thức ---
    @Transactional
    public RecipeResponse updateRecipe(UUID recipeId, UUID currentUserId, UpdateRecipeRequest request) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));

        // *** KIỂM TRA QUYỀN SỞ HỮU ***
        if (!recipe.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to edit this recipe.");
        }

        // Chỉ cập nhật các trường được cung cấp
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

    // --- PHƯƠNG THỨC MỚI: Xóa công thức ---
    @Transactional
    public void deleteRecipe(UUID recipeId, UUID currentUserId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + recipeId));

        // *** KIỂM TRA QUYỀN SỞ HỮU ***
        if (!recipe.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this recipe.");
        }

        recipeRepository.delete(recipe);
    }
    // === PHƯƠNG THỨC MỚI ĐỂ GỌI VÀ LOG AI ===
    public Mono<AnalyzeDishResponse> analyzeDishAndLog(MultipartFile image, String context, UUID userId) {
        long startTime = System.currentTimeMillis();

        return aiClientService.analyzeDish(image, context)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    // Cắt bớt response để log không quá dài
                    String responseSummary = response.getDescription().length() > 200 ?
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