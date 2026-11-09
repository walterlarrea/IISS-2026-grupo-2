package com.iiss.sensores.grupo_2.controlador.engine.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Creates a client for SwitchClient */
@Configuration
public class SwitchRestClientConfig {
  /** A switch that does not answer must not block the control cycle indefinitely */
  private static final int TIMEOUT_MS = 3000;

  @Bean
  public RestClient switchRestClient(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(TIMEOUT_MS);
    requestFactory.setReadTimeout(TIMEOUT_MS);
    return builder
        .clone()
        .requestFactory(requestFactory)
        .defaultHeader("Accept", "application/json")
        .build();
  }
}
