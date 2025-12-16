package com.cinetaste.recipeservice.controller;

import com.cinetaste.recipeservice.dto.*;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.service.AiFeedbackService;
import com.cinetaste.recipeservice.service.RecipeService;
import com.cinetaste.recipeservice.service.CommentReactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final CommentReactionService commentReactionService; // ✅ THÊM DÒNG NÀY

    @PostMapping
    public ResponseEntity<Recipe> createRecipe(
            @Valid @RequestBody CreateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID authorId = UUID.fromString(userIdHeader);
        Recipe createdRecipe = recipeService.createRecipe(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable));
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeDetailResponse> getRecipeById(@PathVariable UUID recipeId) {
        try {
            RecipeDetailResponse recipeDetail = recipeService.getRecipeDetailById(recipeId);
            return ResponseEntity.ok(recipeDetail);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{recipeId}/ratings")
    public ResponseEntity<Void> rateRecipe(
            @PathVariable UUID recipeId,
            @Valid @RequestBody RateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        recipeService.rateRecipe(recipeId, request, userId);
        return ResponseEntity.ok().build();
    }

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

    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getComments(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

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

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<RecipeResponse>> getRecipesByAuthor(
            @PathVariable UUID authorId,
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(recipeService.getRecipesByAuthorId(authorId, pageable));
    }

    // ============================================================================
    // ✅ ENDPOINT MỚI: REACT TO COMMENT
    // ============================================================================
    @PostMapping("/{recipeId}/comments/{commentId}/reactions")
    public ResponseEntity<CommentReactionDto> reactToComment(
            @PathVariable UUID recipeId,
            @PathVariable Long commentId,
            @RequestHeader("X-User-ID") String userIdHeader,
            @Valid @RequestBody ReactToCommentRequest request
    ) {
        try {
            System.out.println("📥 POST /api/recipes/" + recipeId + "/comments/" + commentId + "/reactions");
            System.out.println("📦 Reaction: " + request.getReactionType());

            UUID userId = UUID.fromString(userIdHeader);
            CommentReactionDto result = commentReactionService.reactToComment(
                    commentId,
                    userId,
                    request.getReactionType()
            );

            System.out.println("✅ Backend trả về: " + result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}