package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.CommentResponse;
import com.cinetaste.recipeservice.dto.CreateRecipeRequest;
import com.cinetaste.recipeservice.dto.RateRecipeRequest;
import com.cinetaste.recipeservice.dto.RecipeResponse;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.entity.RecipeRating;
import com.cinetaste.recipeservice.entity.RecipeRatingId;
import com.cinetaste.recipeservice.repository.CommentRepository;
import com.cinetaste.recipeservice.repository.RecipeRatingRepository;
import com.cinetaste.recipeservice.repository.RecipeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cinetaste.recipeservice.entity.RecipeRating;
import com.cinetaste.recipeservice.dto.CommentResponse;
import com.cinetaste.recipeservice.dto.CreateCommentRequest;
import com.cinetaste.recipeservice.entity.Comment;
import com.cinetaste.recipeservice.repository.CommentRepository;

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
    public List<RecipeResponse> getAllRecipes() {
        return recipeRepository.findAll()
                .stream()
                .map(this::mapToRecipeResponse)
                .collect(Collectors.toList());
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
    // --- HÀM MỚI ---
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
    // --- HÀM MỚI: Thêm bình luận ---
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
}