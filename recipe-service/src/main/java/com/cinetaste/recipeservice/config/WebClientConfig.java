package com.cinetaste.recipeservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit; // Thêm import này

@Configuration
public class WebClientConfig {

    @Value("${ai.service.base-url}") // URL này nên trỏ đến API Gateway
    private String aiServiceBaseUrl;

    @Bean
    public WebClient aiWebClient() {
        // Cấu hình HttpClient với connection pooling và timeouts
        HttpClient httpClient = HttpClient.create()
                // Thời gian chờ tối đa để thiết lập kết nối TCP
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 giây
                .responseTimeout(Duration.ofSeconds(120)) // Timeout chờ response từ AI (tăng lên 120s)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)) // Timeout đọc data
                                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))); // Timeout gửi data

        // Cấu hình Connection Pool (Mặc định Reactor Netty có pool rồi, nhưng có thể tùy chỉnh thêm)
        // .poolResources(pool -> pool.maxConnections(500).pendingAcquireTimeout(Duration.ofSeconds(60)))

        return WebClient.builder()
                .baseUrl(aiServiceBaseUrl) // Sử dụng URL trỏ đến Gateway
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Tăng giới hạn buffer (nếu AI trả về JSON lớn) - tùy chọn
                // .exchangeStrategies(ExchangeStrategies.builder()
                //     .codecs(configurer -> configurer
                //         .defaultCodecs()
                //         .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                //     .build())
                .build();
    }
}