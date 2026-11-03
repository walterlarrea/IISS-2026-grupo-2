package com.iiss.sensores.grupo_2.controlador.api;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.iiss.sensores.grupo_2.controlador.api.config.ApiKeyRoom;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica la autorización de solicitudes mediante API key. */
class ApiKeyTest {

  private static final String VALID_KEY = "dev-rooms-key";
  private ApiKeyRoom apiKeyInterceptor;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  /** Prepara un interceptor y solicitudes/respuestas HTTP simuladas para cada caso. */
  @BeforeEach
  void setUp() {
    apiKeyInterceptor = new ApiKeyRoom(VALID_KEY);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  /** Comprueba que se rechace una solicitud sin encabezado de API key. */
  @Test
  @DisplayName("Devuelve 401 Unauthorized cuando la clave API está ausente")
  void deniesApiKeyRequestAbsent() throws Exception {
    boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  /** Comprueba que se rechace una API key compuesta solo por espacios. */
  @Test
  @DisplayName("Devuelve 401 Unauthorized cuando la clave API está en blanco")
  void deniesApiKeyBlankSpace() throws Exception {
    request.addHeader("X-API-KEY", "   ");

    boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  /** Comprueba que una clave incorrecta produzca el error JSON esperado. */
  @Test
  @DisplayName(
      "Devuelve 401 Unauthorized con el formato de error documentado cuando la clave es incorrecta")
  void deniesApiKeyRequestIncorrect() throws Exception {
    request.addHeader("X-API-KEY", "clave-invalida");

    boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertTrue(
        response
            .getContentAsString()
            .contains("\"mensaje\":\"falta o no coincide el header X-API-Key\""));
  }

  /** Comprueba que una solicitud con la clave correcta sea autorizada. */
  @Test
  @DisplayName("Permite el acceso cuando la clave API es correcta")
  void allowsApiKeyRequest() throws Exception {
    request.addHeader("X-API-KEY", VALID_KEY);

    boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }
}
