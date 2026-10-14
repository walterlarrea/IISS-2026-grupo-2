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

@SpringBootTest(properties = "logging.level.root=INFO")
@AutoConfigureMockMvc
@ActiveProfiles("development")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SwitchControllerTest {

    private static final String API_KEY = "development-key";

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // GET /switches/{id}
    // -------------------------------------------------------------------------

    @Test
    void creaUnSwitchApagadoAlConsultarUnIdNuevo() throws Exception {
        mockMvc.perform(get("/switches/nuevo").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSwitch").value("nuevo"))
                .andExpect(jsonPath("$.estado").value("OFF"));
    }

    @Test
    void retornaElMismoIdQueSeConsulto() throws Exception {
        mockMvc.perform(get("/switches/cocina-luz").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSwitch").value("cocina-luz"));
    }

    // -------------------------------------------------------------------------
    // POST /switches/{id}/comandos
    // -------------------------------------------------------------------------

    @Test
    void actualizaElEstadoDelSwitchAEncendido() throws Exception {
        mockMvc.perform(post("/switches/sala-1/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"ON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSwitch").value("sala-1"))
                .andExpect(jsonPath("$.estado").value("ON"))
                .andExpect(jsonPath("$.cambio").value(true));
    }

    @Test
    void actualizaElEstadoDelSwitchAApagado() throws Exception {
        // Primero encendemos
        mockMvc.perform(post("/switches/sala-2/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"ON\"}"))
                .andExpect(status().isOk());

        // Luego apagamos y verificamos
        mockMvc.perform(post("/switches/sala-2/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"OFF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OFF"))
                .andExpect(jsonPath("$.cambio").value(true));
    }

    @Test
    void indicaQueNoHuboCambioSiSeRepiteElEstado() throws Exception {
        mockMvc.perform(post("/switches/sala-4/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"ON\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/switches/sala-4/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"ON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ON"))
                .andExpect(jsonPath("$.cambio").value(false));
    }

    @Test
    void actualizaYConsultaElEstadoDelSwitch() throws Exception {
        mockMvc.perform(post("/switches/sala-3/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"ON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ON"));

        mockMvc.perform(get("/switches/sala-3").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ON"));
    }

    // -------------------------------------------------------------------------
    // Validación de body
    // -------------------------------------------------------------------------

    @Test
    void rechazaBodyConAccionInvalida() throws Exception {
        mockMvc.perform(post("/switches/sala-1/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"accion\":\"INVALIDA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaBodySinCampoAccion() throws Exception {
        mockMvc.perform(post("/switches/sala-1/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaBodyVacio() throws Exception {
        mockMvc.perform(post("/switches/sala-1/comandos")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content(""))
                .andExpect(status().isBadRequest());
    }

}