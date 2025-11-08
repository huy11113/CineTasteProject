package com.cinetaste.recipeservice.controller;

import com.cinetaste.recipeservice.dto.*;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.service.AiFeedbackService;
import com.cinetaste.recipeservice.service.RecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Đảm bảo import này
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // (Hàm createRecipe không đổi)
    @PostMapping
    public ResponseEntity<Recipe> createRecipe(
            @Valid @RequestBody CreateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID authorId = UUID.fromString(userIdHeader);
        Recipe createdRecipe = recipeService.createRecipe(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    // (Hàm getAllRecipes không đổi - trả về tóm tắt)
    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable));
    }

    // --- SỬA HÀM NÀY (Lỗi 1) ---
    // Trả về RecipeDetailResponse (đầy đủ chi tiết)
    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailResponse> getRecipeById(@PathVariable UUID recipeId) { // <-- Sửa kiểu trả về
        try {
            // Sửa tên hàm được gọi cho khớp với Service
            RecipeDetailResponse recipeDetail = recipeService.getRecipeDetailById(recipeId);
            return ResponseEntity.ok(recipeDetail);
        } catch (RuntimeException e) {
            // TODO: Phân biệt lỗi Not Found và lỗi khác
            return ResponseEntity.notFound().build();
        }
    }
    // --- KẾT THÚC SỬA ---

    // (Hàm rateRecipe không đổi)
    @PostMapping("/{recipeId}/ratings")
    public ResponseEntity<Void> rateRecipe(
            @PathVariable UUID recipeId,
            @Valid @RequestBody RateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        recipeService.rateRecipe(recipeId, request, userId);
        return ResponseEntity.ok().build();
    }

    // (Hàm addComment không đổi)
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

    // (Hàm getComments không đổi)
    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getComments(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // (Hàm updateRecipe không đổi)
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

    // (Hàm deleteRecipe không đổi)
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable UUID recipeId,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID currentUserId = UUID.fromString(userIdHeader);
        try {
            recipeService.deleteRecipe(recipeId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // (Các hàm AI không đổi)
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