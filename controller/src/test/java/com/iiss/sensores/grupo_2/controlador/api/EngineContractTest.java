package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.StateControl;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "CONTROLADOR_API_KEY=test-api-key",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "mqtt.broker-url=tcp://localhost:1883",
        "mqtt.client-id=ContractTestClient",
        "mqtt.topic=test/topic"
})
@AutoConfigureMockMvc
class EngineContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private ControllerTempAuto engineService;

    @MockitoBean
    private SwitchClient switchClient;

    @Test
    void obtieneSitioYDevuelve404CuandoAunNoFueConfigurado() throws Exception {
        when(inventoryService.obtenerActivo()).thenReturn(Optional.empty());

        mockMvc.perform(get("/site").header("X-API-KEY", "test-api-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void reemplazaYConsultaSitio() throws Exception {
        String payload = """
                {
                  "schemaVersion": "2.0",
                  "sitio": {
                    "id": "casa",
                    "nombre": "Casa",
                    "potenciaContratadaKW": 3.7,
                    "tarifa": {"punta": {"desde": "17:00", "hasta": "23:00", "dias": "HABILES"}}
                  },
                  "habitaciones": [{
                    "id": "room1",
                    "nombre": "Living",
                    "temperaturaEsperada": 21.5,
                    "potenciaKW": 1.2,
                    "idTermostato": "termo1",
                    "topicTermostato": "termo1/status/temperature:0",
                    "idSwitch": "switch1",
                    "urlSwitch": "http://switches:8081"
                  }]
                }
                """;
        when(inventoryService.reemplazar(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(switchClient.consultarEstado(any()))
                .thenReturn(new SwitchStateResponse("switch1", SwitchState.APAGADO, null));

        mockMvc.perform(put("/site")
                        .header("X-API-KEY", "test-api-key")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.site").value("casa"))
                .andExpect(jsonPath("$.rooms").value(1));

        mockMvc.perform(put("/site")
                        .header("X-API-KEY", "test-api-key")
                        .contentType("application/json")
                        .content(payload.replace("\"schemaVersion\": \"2.0\"", "\"schemaVersion\": \"1.0\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void comandosSincronizanYExponenEstadoDelControl() throws Exception {
        Instant fechaHora = Instant.parse("2026-10-01T09:15:03.120Z");
        when(engineService.getTempControllerStatus()).thenReturn(new StateControl(
                StateControl.Estado.EN_EJECUCION,
                fechaHora,
                10,
                Instant.parse("2026-10-01T09:15:00Z")
        ));

        mockMvc.perform(post("/control/commands")
                        .header("X-API-KEY", "test-api-key")
                        .contentType("application/json")
                        .content("""
                                {"accion":"INICIAR","fechaHora":"2026-10-01T09:15:03.120Z","factorTiempo":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_EJECUCION"))
                .andExpect(jsonPath("$.factorTiempo").value(10));

        verify(engineService).start(fechaHora, 10);

        mockMvc.perform(post("/control/commands")
                        .header("X-API-KEY", "test-api-key")
                        .contentType("application/json")
                        .content("{\"accion\":\"INICIAR\",\"factorTiempo\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void protegeLosEndpointsNuevosConElFormatoDe401Documentado() throws Exception {
        mockMvc.perform(get("/control"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("falta o no coincide el header X-API-Key"));
    }
}
