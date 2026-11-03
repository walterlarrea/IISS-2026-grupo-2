package com.iiss.sensores.grupo_2.controlador.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor de API key para las rutas protegidas del controlador. */
@Configuration
public class WebConfigRoom implements WebMvcConfigurer {
  private final ApiKeyRoom apiKeyInterceptor;

  /** Crea la configuración web con el interceptor de autenticación. */
  public WebConfigRoom(ApiKeyRoom apiKeyInterceptor) {
    this.apiKeyInterceptor = apiKeyInterceptor;
  }

  /** Aplica la autenticación a las rutas de negocio y excluye la documentación. */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(apiKeyInterceptor)
        .addPathPatterns("/habitaciones/**", "/controller/**", "/sitio", "/control/**")
        .excludePathPatterns("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/docs/**");
  }
}
