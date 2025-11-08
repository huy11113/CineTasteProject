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
@IdClass(AiRequestsLogId.class) // <-- THÊM DÒNG NÀY
public class AiRequestsLog {

    @Id // <-- SỬA
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String feature;

    @Column(name = "request_payload_summary", columnDefinition = "TEXT")
    private String requestPayloadSummary;

    @Column(name = "response_payload_summary", columnDefinition = "TEXT")
    private String responsePayloadSummary;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Id // <-- THÊM DÒNG NÀY
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}