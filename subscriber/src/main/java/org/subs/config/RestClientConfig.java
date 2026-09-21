package org.subs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient roomRestClient(RestClient.Builder builder,
                                     @Value("${ROOMS_API_URL:http://rooms-api:8080}") String roomsApiUrl,
                                     @Value("${ROOMS_API_KEY:dev-rooms-key}") String roomsApiKey) {
        return builder.clone().baseUrl(roomsApiUrl).defaultHeader("X-API-Key", roomsApiKey).build();
    }

    @Bean
    public RestClient switchRestClient(RestClient.Builder builder,
                                       @Value("${SWITCHES_API_URL:http://switches-api:8090}") String switchesApiUrl,
                                       @Value("${SWITCHES_API_KEY:development-key}") String switchesApiKey) {
        return builder.clone().baseUrl(switchesApiUrl).defaultHeader("X-API-KEY", switchesApiKey).build();
    }
}
