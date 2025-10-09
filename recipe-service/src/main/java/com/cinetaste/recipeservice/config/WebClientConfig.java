package com.cinetaste.recipeservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder().baseUrl(aiServiceBaseUrl).build();
    }
}