package com.cinetaste.recipeservice.client;

import com.cinetaste.recipeservice.dto.tmdb.TmdbMovieResult;
import com.cinetaste.recipeservice.dto.tmdb.TmdbResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TmdbClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    public TmdbMovieResult searchMovie(String query) {
        if (query == null || query.trim().isEmpty()) return null;

        try {
            TmdbResponse response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/search/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbResponse.class)
                    .block(); // Gọi đồng bộ

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                // Lấy kết quả đầu tiên (phù hợp nhất)
                return response.getResults().get(0);
            }
        } catch (Exception e) {
            System.err.println("Error calling TMDB: " + e.getMessage());
        }
        return null;
    }
}