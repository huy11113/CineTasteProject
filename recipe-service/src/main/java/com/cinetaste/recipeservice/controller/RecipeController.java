package com.cinetaste.recipeservice.controller;

import com.cinetaste.recipeservice.dto.CreateRecipeRequest;
import com.cinetaste.recipeservice.dto.RateRecipeRequest;
import com.cinetaste.recipeservice.dto.RecipeResponse;
import com.cinetaste.recipeservice.entity.Recipe;
import com.cinetaste.recipeservice.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.cinetaste.recipeservice.dto.CommentResponse;
import com.cinetaste.recipeservice.dto.CreateCommentRequest;
// Bỏ import của Spring Security vì không cần nữa
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

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
    // --- ENDPOINT MỚI: Thêm một bình luận ---
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

    // --- ENDPOINT MỚI: Lấy tất cả bình luận của một công thức ---
    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getComments(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}