package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_requests_log")
public class AiRequestsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String feature;

    // === PHẦN ĐÃ SỬA ===
    @Column(name = "request_payload_summary", columnDefinition = "TEXT")
    private String requestPayloadSummary;

    // === PHẦN ĐÃ SỬA ===
    @Column(name = "response_payload_summary", columnDefinition = "TEXT")
    private String responsePayloadSummary;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(nullable = false)
    private boolean success;

    // === PHẦN ĐÃ SỬA ===
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}