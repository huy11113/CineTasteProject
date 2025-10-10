package com.cinetaste.recipeservice.controller;

import com.cinetaste.recipeservice.dto.*;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.service.AiFeedbackService;
import com.cinetaste.recipeservice.service.RecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Bỏ import của Spring Security vì không cần nữa
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final AiFeedbackService aiFeedbackService;
    @PostMapping
    public ResponseEntity<Recipe> createRecipe(
            @Valid @RequestBody CreateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) { // <-- THAY ĐỔI: Nhận header từ Gateway

        // Chuyển đổi string sang UUID
        UUID authorId = UUID.fromString(userIdHeader);
        Recipe createdRecipe = recipeService.createRecipe(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getRecipeById(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{recipeId}/ratings")
    public ResponseEntity<Void> rateRecipe(
            @PathVariable UUID recipeId,
            @Valid @RequestBody RateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) { // <-- THAY ĐỔI: Nhận header từ Gateway

        UUID userId = UUID.fromString(userIdHeader);
        recipeService.rateRecipe(recipeId, request, userId);
        return ResponseEntity.ok().build();
    }
    // --- ENDPOINT MỚI: Thêm một bình luận --
    @PostMapping("/{recipeId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID recipeId,
            @RequestHeader("X-User-ID") String userIdHeader,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        UUID authorId = UUID.fromString(userIdHeader);
        CommentResponse newComment = recipeService.addComment(recipeId, authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }

    // --- ENDPOINT MỚI: Lấy tất cả bình luận của một công thức --
    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getComments(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // --- ENDPOINT MỚI: Cập nhật một công thức ---
    @PutMapping("/{recipeId}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID recipeId,
            @RequestHeader("X-User-ID") String userIdHeader,
            @Valid @RequestBody UpdateRecipeRequest request) {

        UUID currentUserId = UUID.fromString(userIdHeader);
        try {
            RecipeResponse updatedRecipe = recipeService.updateRecipe(recipeId, currentUserId, request);
            return ResponseEntity.ok(updatedRecipe);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- ENDPOINT MỚI: Xóa một công thức ---
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable UUID recipeId,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID currentUserId = UUID.fromString(userIdHeader);
        try {
            recipeService.deleteRecipe(recipeId, currentUserId);
            return ResponseEntity.noContent().build(); // Trả về 204 No Content là chuẩn cho việc xóa thành công
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // === CHUYỂN ENDPOINT TỪ AiController VÀO ĐÂY ===

    @PostMapping(value = "/ai/analyze-dish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AnalyzeDishResponse>> analyzeDish(
            @RequestHeader("X-User-ID") String userIdHeader,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "context", required = false) String context
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        return recipeService.analyzeDishAndLog(image, context, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/ai/feedback")
    public ResponseEntity<Void> submitFeedback(
            @RequestHeader("X-User-ID") String userIdHeader,
            @RequestBody AiFeedbackRequest feedbackRequest
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        aiFeedbackService.saveFeedback(userId, feedbackRequest);
        return ResponseEntity.ok().build();
    }
}
