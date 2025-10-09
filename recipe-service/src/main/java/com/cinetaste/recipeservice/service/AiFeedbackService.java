package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.AiFeedbackRequest;
import com.cinetaste.recipeservice.entity.AiFeedback;
import com.cinetaste.recipeservice.repository.AiFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;

    public void saveFeedback(UUID userId, AiFeedbackRequest request) {
        AiFeedback feedback = new AiFeedback();
        feedback.setUserId(userId);
        feedback.setRecipeId(request.getRecipeId());
        feedback.setAiFeature(request.getAiFeature());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        aiFeedbackRepository.save(feedback);
    }
}