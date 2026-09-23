package org.room.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfigRoom implements WebMvcConfigurer {
    private final ApiKeyRoom apiKeyInterceptor;

    public WebConfigRoom(ApiKeyRoom apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .excludePathPatterns("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/docs/**");
    }
}
