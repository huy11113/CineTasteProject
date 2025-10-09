package com.cinetaste.recipeservice.service;

import com.cinetaste.recipeservice.dto.ai.AnalyzeDishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiClientService {

    private final WebClient aiWebClient;

    public Mono<AnalyzeDishResponse> analyzeDish(MultipartFile image, String context) {
        return aiWebClient.post()
                .uri("/analyze-dish")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(buildMultipartBody(image, context)))
                .retrieve()
                .bodyToMono(AnalyzeDishResponse.class);
    }

    private MultiValueMap<String, Object> buildMultipartBody(MultipartFile image, String context) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            if (context != null && !context.isEmpty()) {
                body.add("context", context);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read image file", e);
        }
        return body;
    }
}