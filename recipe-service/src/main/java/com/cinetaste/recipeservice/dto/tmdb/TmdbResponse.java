package com.cinetaste.recipeservice.dto.tmdb;

import lombok.Data;
import java.util.List;

@Data
public class TmdbResponse {
    private List<TmdbMovieResult> results;
}