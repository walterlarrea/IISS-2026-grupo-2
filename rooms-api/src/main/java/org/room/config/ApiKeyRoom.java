package org.room.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyRoom implements HandlerInterceptor {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private final String expectedApiKey;

    public ApiKeyRoom(@Value("${ROOMS_API_KEY}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (providedApiKey == null || providedApiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!expectedApiKey.equals(providedApiKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
