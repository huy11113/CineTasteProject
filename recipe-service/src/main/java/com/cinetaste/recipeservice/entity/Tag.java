package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp; // <-- Thêm import
import java.time.Instant; // <-- Thêm import
import java.util.ArrayList; // <-- Thêm import
import java.util.List; // <-- Thêm import

@Data
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String type;
    @Column(name = "image_url")
    private String imageUrl;
    // --- THÊM CÁC TRƯỜNG MỚI ---
    @Column(unique = true, nullable = false, length = 50)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeTag> recipeTags = new ArrayList<>();
    // --- KẾT THÚC THÊM ---
}