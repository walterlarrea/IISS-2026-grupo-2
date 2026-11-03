package com.iiss.sensores.grupo_2.controlador.engine.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Configura el cliente REST usado para comunicarse con la API de switches. */
@Configuration
public class SwitchRestClientConfig {

  /** Crea el cliente REST con el encabezado de aceptación JSON. */
  @Bean
  public RestClient switchRestClient(RestClient.Builder builder) {
    return builder.clone().defaultHeader("Accept", "application/json").build();
  }
}
