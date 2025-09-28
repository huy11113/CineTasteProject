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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @AuthenticationPrincipal UserDetails userDetails) {

        // Lấy username của người dùng đã được xác thực từ token
        String username = userDetails.getUsername();


        // Hiện tại, recipe-service không biết UUID của user.
        // Cách làm tạm thời: chúng ta cần gọi sang user-service để lấy UUID từ username.
        // Hoặc đơn giản hơn bây giờ, chúng ta sẽ hard-code một UUID để test.
        UUID authorId = UUID.fromString("573f81b9-4512-4666-84cf-0595ac12c87b"); 
        Recipe createdRecipe = recipeService.createRecipe(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }
    // --- ENDPOINT MỚI ---
    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    // --- ENDPOINT MỚI ---
    @GetMapping("/{recipeId}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID recipeId) {
        try {
            return ResponseEntity.ok(recipeService.getRecipeById(recipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // --- ENDPOINT MỚI ---
    @PostMapping("/{recipeId}/ratings")
    public ResponseEntity<Void> rateRecipe(
            @PathVariable UUID recipeId,
            @Valid @RequestBody RateRecipeRequest request,
            @RequestHeader("X-User-ID") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        recipeService.rateRecipe(recipeId, request, userId);
        return ResponseEntity.ok().build();
    }
}