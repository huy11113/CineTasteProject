package com.cinetaste.recipeservice.controller;

import com.cinetaste.recipeservice.dto.AiFeedbackRequest;
import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import com.cinetaste.recipeservice.service.AiFeedbackService;
import com.cinetaste.recipeservice.service.RecipeService; // THAY ĐỔI
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFeedbackService aiFeedbackService;
    private final RecipeService recipeService; // THAY ĐỔI: Sử dụng RecipeService

    @PostMapping(value = "/analyze-dish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AnalyzeDishResponse>> analyzeDish(
            @RequestHeader("X-User-ID") String userIdHeader,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "context", required = false) String context
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        // THAY ĐỔI: Gọi phương thức trong RecipeService
        return recipeService.analyzeDishAndLog(image, context, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(
            @RequestHeader("X-User-ID") String userIdHeader,
            @RequestBody AiFeedbackRequest feedbackRequest
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        aiFeedbackService.saveFeedback(userId, feedbackRequest);
        return ResponseEntity.ok().build();
    }
}