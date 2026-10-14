package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.service.EstadoSitioService;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchEstadoResponse;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ControllerTempAutoTest {
    @Mock
    private SwitchClient switchClient;
    @Mock
    private ICore core;
    private ControllerTempAuto controller;

    @Test
    void temperaturaActualizadaLlegaAlCore() {
        when(switchClient.consultarEstado(any())).thenReturn(new SwitchEstadoResponse("switch-1", EstadoSwitch.APAGADO, null));
        EstadoSitioService estadoService = new EstadoSitioService(switchClient);

        estadoService.cargarEstado(new DataSitio(
                new Sitio("sitio-1", "Casa", 5.0, new Tarifa(null)),
                List.of(new Habitacion(
                        "hab-1",
                        "Living",
                        21.0,
                        1.5,
                        "termo-1",
                        "casa/living/temperatura",
                        "switch-1",
                        "http://localhost/switches/1",
                        19.0,
                        EstadoSwitch.APAGADO
                ))
        ));

        controller = new ControllerTempAuto(switchClient, core, estadoService);

        when(core.calcularComandos(any(), any())).thenReturn(new RespuestaCore(List.of()));

        controller.iniciar(Instant.now(), 1.0);
        controller.procesarTemperatura("casa/living/temperatura", 18.0);

        ArgumentCaptor<DataSitio> captor = ArgumentCaptor.forClass(DataSitio.class);

        verify(core).calcularComandos(any(), captor.capture());

        assertEquals(18.0, captor.getValue().habitaciones().get(0).temperaturaActual());
    }
}
