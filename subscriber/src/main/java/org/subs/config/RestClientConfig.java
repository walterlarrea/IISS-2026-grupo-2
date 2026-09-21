package org.subs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient restClient(RestClient.Builder builder, @Value("${ROOMS_API_URL}") String roomsApiUrl, @Value("${ROOMS_API_KEY}") String roomsApiKey) {
        return builder.baseUrl(roomsApiUrl).defaultHeader("X-API-Key", roomsApiKey).build();
    }
}
