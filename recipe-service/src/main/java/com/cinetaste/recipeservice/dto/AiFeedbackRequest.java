package com.cinetaste.recipeservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AiFeedbackRequest {
    private UUID recipeId;
    private String aiFeature;
    private short rating;
    private String comment;
}