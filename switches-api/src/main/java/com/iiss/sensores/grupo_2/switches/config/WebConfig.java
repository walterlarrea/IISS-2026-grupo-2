package com.iiss.sensores.grupo_2.switches.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configura el acceso CORS a los endpoints de la API. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${rooms.swagger-ui.origin:http://localhost:8080}")
  private String roomsSwaggerOrigin;

  /** Crea la configuración web de la API. */
  public WebConfig() {}

  /** Permite solicitudes HTTP desde el origen configurado. */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(roomsSwaggerOrigin)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}
