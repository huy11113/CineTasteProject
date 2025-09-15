package com.cinetaste.userservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data // Annotation của Lombok để tự tạo Getters, Setters, toString, equals, hashCode
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = false, length = 20)
    private String role = "user";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}