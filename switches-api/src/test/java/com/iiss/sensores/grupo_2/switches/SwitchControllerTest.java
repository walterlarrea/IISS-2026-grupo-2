package com.iiss.sensores.grupo_2.switches;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica los endpoints REST para consultar y comandar switches. */
@SpringBootTest(properties = "logging.level.root=INFO")
@AutoConfigureMockMvc
@ActiveProfiles("development")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SwitchControllerTest {

  private static final String API_KEY = "development-key";

  @Autowired private MockMvc mockMvc;

  // -------------------------------------------------------------------------
  // GET /switches/{id}
  // -------------------------------------------------------------------------

  /** Comprueba que consultar un switch nuevo lo inicialice apagado. */
  @Test
  void creaUnSwitchApagadoAlConsultarUnIdNuevo() throws Exception {
    mockMvc
        .perform(get("/switches/nuevo").header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idSwitch").value("nuevo"))
        .andExpect(jsonPath("$.estado").value("OFF"));
  }

  /** Comprueba que la consulta devuelva el mismo identificador solicitado. */
  @Test
  void retornaElMismoIdQueSeConsulto() throws Exception {
    mockMvc
        .perform(get("/switches/cocina-luz").header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idSwitch").value("cocina-luz"));
  }

  // -------------------------------------------------------------------------
  // POST /switches/{id}/comandos
  // -------------------------------------------------------------------------

  /** Comprueba que el endpoint de comandos encienda el switch. */
  @Test
  void actualizaElEstadoDelSwitchAEncendido() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-1/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"ON\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idSwitch").value("sala-1"))
        .andExpect(jsonPath("$.estado").value("ON"))
        .andExpect(jsonPath("$.cambio").value(true));
  }

  /** Comprueba que el endpoint permita apagar un switch previamente encendido. */
  @Test
  void actualizaElEstadoDelSwitchAApagado() throws Exception {
    // Primero encendemos
    mockMvc
        .perform(
            post("/switches/sala-2/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"ON\"}"))
        .andExpect(status().isOk());

    // Luego apagamos y verificamos
    mockMvc
        .perform(
            post("/switches/sala-2/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"OFF\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("OFF"))
        .andExpect(jsonPath("$.cambio").value(true));
  }

  /** Comprueba que repetir el mismo estado no reporte un cambio. */
  @Test
  void indicaQueNoHuboCambioSiSeRepiteElEstado() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-4/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"ON\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/switches/sala-4/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"ON\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ON"))
        .andExpect(jsonPath("$.cambio").value(false));
  }

  /** Comprueba que el estado aplicado pueda consultarse después del comando. */
  @Test
  void actualizaYConsultaElEstadoDelSwitch() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-3/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"ON\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ON"));

    mockMvc
        .perform(get("/switches/sala-3").header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ON"));
  }

  // -------------------------------------------------------------------------
  // Validación de body
  // -------------------------------------------------------------------------

  /** Comprueba que se rechace una acción que no pertenece a los estados admitidos. */
  @Test
  void rechazaBodyConAccionInvalida() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-1/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{\"accion\":\"INVALIDA\"}"))
        .andExpect(status().isBadRequest());
  }

  /** Comprueba que se rechace un cuerpo sin el campo de acción obligatorio. */
  @Test
  void rechazaBodySinCampoAccion() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-1/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  /** Comprueba que se rechace un cuerpo vacío. */
  @Test
  void rechazaBodyVacio() throws Exception {
    mockMvc
        .perform(
            post("/switches/sala-1/comandos")
                .header("X-API-KEY", API_KEY)
                .contentType("application/json")
                .content(""))
        .andExpect(status().isBadRequest());
  }
}
