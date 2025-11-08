package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal; // <-- Thêm import
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "movies")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Integer tmdbId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "poster_url")
    private String posterUrl;

    // --- THÊM CÁC TRƯỜNG MỚI ---
    @Column(name = "backdrop_url")
    private String backdropUrl;

    private Integer runtime;

    @Column(name = "vote_average", precision = 3, scale = 1)
    private BigDecimal voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;
    // --- KẾT THÚC THÊM ---

    @Column(columnDefinition = "jsonb")
    private String genres;

    // (createdAt và updatedAt sẽ được tự động quản lý bởi Trigger trong CSDL)
}