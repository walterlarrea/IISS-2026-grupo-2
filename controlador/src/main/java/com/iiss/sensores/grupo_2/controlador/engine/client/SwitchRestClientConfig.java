package com.iiss.sensores.grupo_2.controlador.engine.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SwitchRestClientConfig {

    @Bean
    public RestClient switchRestClient(RestClient.Builder builder) {
        return builder.clone().defaultHeader("Accept", "application/json").build();
    }
}
