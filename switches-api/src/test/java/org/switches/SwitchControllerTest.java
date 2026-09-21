package org.switches;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("development")
class SwitchControllerTest {

    private static final String API_KEY = "development-key";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void creaUnSwitchApagadoAlConsultarUnIdNuevo() throws Exception {
        mockMvc.perform(get("/switches/nuevo").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("nuevo"))
                .andExpect(jsonPath("$.encendido").value(false));
    }

    @Test
    void actualizaYConsultaElEstadoDelSwitch() throws Exception {
        mockMvc.perform(post("/switches")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"id\":\"sala-1\",\"encendido\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encendido").value(true));

        mockMvc.perform(get("/switches/sala-1").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encendido").value(true));
    }

    @Test
    void rechazaBodyConTiposInvalidos() throws Exception {
        mockMvc.perform(post("/switches")
                        .header("X-API-KEY", API_KEY)
                        .contentType("application/json")
                        .content("{\"id\":\"sala-1\",\"encendido\":\"si\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distingueClaveAusenteDeClaveIncorrecta() throws Exception {
        mockMvc.perform(get("/switches/sala-1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/switches/sala-1").header("X-API-KEY", "incorrecta"))
                .andExpect(status().isForbidden());
    }
}