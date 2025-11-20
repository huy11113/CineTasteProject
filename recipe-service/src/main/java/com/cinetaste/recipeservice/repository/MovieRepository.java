package com.cinetaste.recipeservice.repository;

import com.cinetaste.recipeservice.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    // Tìm phim dựa trên ID của TMDB
    Optional<Movie> findByTmdbId(Integer tmdbId);
}