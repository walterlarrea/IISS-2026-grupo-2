package com.iiss.sensores.grupo_2.controlador.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiss.sensores.grupo_2.controlador.api.model.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;

@Component
public class ApiKeyRoom implements HandlerInterceptor {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private final String expectedApiKey;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiKeyRoom(
            @Value("${CONTROLADOR_API_KEY:development-key}") String expectedApiKey,
            ObjectMapper objectMapper
    ) {
        this.expectedApiKey = expectedApiKey;
        this.objectMapper = objectMapper;
    }

    public ApiKeyRoom(String expectedApiKey) {
        this(expectedApiKey, new ObjectMapper().findAndRegisterModules());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (providedApiKey == null || providedApiKey.isBlank()) {
            responderNoAutorizado(response);
            return false;
        }
        if (!expectedApiKey.equals(providedApiKey)) {
            responderNoAutorizado(response);
            return false;
        }
        return true;
    }

    private void responderNoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorApi(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "falta o no coincide el header X-API-Key",
                null
        ));
    }
}
