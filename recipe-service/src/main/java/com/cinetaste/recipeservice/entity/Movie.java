package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal; // <-- Import quan trọng
import java.time.LocalDate; // <-- Import quan trọng
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

    @Column(nullable = false) // Đảm bảo title không null
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "poster_url")
    private String posterUrl;

    // --- CÁC TRƯỜNG MỚI (KHỚP VỚI CSDL) ---
    @Column(name = "backdrop_url")
    private String backdropUrl;

    private Integer runtime;

    @Column(name = "vote_average", precision = 3, scale = 1)
    private BigDecimal voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;

    @Column(name = "release_date") // <-- Đảm bảo trường này tồn tại
    private LocalDate releaseDate;
    // --- KẾT THÚC TRƯỜNG MỚI ---

    @Column(columnDefinition = "jsonb")
    private String genres;

    // Lưu ý: create_at và updated_at đã được CSDL quản lý bằng trigger
    // nên không cần @CreationTimestamp hay @UpdateTimestamp ở đây.
}