package org.room.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.room.Room;
import org.room.service.HabitacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ROOMS_API_KEY=test-api-key",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HabitacionService service;

    @Test
    @DisplayName("GET /habitaciones/validar devuelve lista de habitaciones no conformes")
    void validarEndpointDevuelveHabitacionesInvalidas() throws Exception {
        Room r1 = new Room("1", "Sala", 21.0, null, "http://switches-api:8090/switches/1");
        when(service.validar()).thenReturn(List.of(r1));

        mockMvc.perform(get("/habitaciones/validar")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].nombre").value("Sala"));
    }
}
