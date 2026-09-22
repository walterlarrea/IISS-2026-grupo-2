package org.switches;

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

@SpringBootTest
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
                .andExpect(jsonPath("$.id").value("nuevo"))
                .andExpect(jsonPath("$.encendido").value(false));
    }

    @Test
    void retornaElMismoIdQueSeConsulto() throws Exception {
        mockMvc.perform(get("/switches/cocina-luz").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cocina-luz"));
    }

    // -------------------------------------------------------------------------
    // POST /switches/{id}
    // -------------------------------------------------------------------------

    @Test
    void actualizaElEstadoDelSwitchAEncendido() throws Exception {
        mockMvc.perform(post("/switches/sala-1")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"encendido\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sala-1"))
                .andExpect(jsonPath("$.encendido").value(true));
    }

    @Test
    void actualizaElEstadoDelSwitchAApagado() throws Exception {
        // Primero encendemos
        mockMvc.perform(post("/switches/sala-2")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"encendido\":true}"))
                .andExpect(status().isOk());

        // Luego apagamos y verificamos
        mockMvc.perform(post("/switches/sala-2")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"encendido\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encendido").value(false));
    }

    @Test
    void actualizaYConsultaElEstadoDelSwitch() throws Exception {
        mockMvc.perform(post("/switches/sala-3")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"encendido\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encendido").value(true));

        mockMvc.perform(get("/switches/sala-3").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encendido").value(true));
    }

    // -------------------------------------------------------------------------
    // Validación de body
    // -------------------------------------------------------------------------

    @Test
    void rechazaBodyConTipoInvalidoEnEncendido() throws Exception {
        mockMvc.perform(post("/switches/sala-1")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"encendido\":\"si\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaBodySinCampoEncendido() throws Exception {
        mockMvc.perform(post("/switches/sala-1")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaBodyVacio() throws Exception {
        mockMvc.perform(post("/switches/sala-1")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content(""))
                .andExpect(status().isBadRequest());
    }

}