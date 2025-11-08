package com.cinetaste.recipeservice.entity;

import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

@Data
public class AiRequestsLogId implements Serializable {
    private Long id;
    private Instant createdAt;
}