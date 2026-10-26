package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ControllerTempAutoTest {
    @Mock
    private SwitchClient switchClient;
    @Mock
    private TemperatureReadingRepository tempReadingsRepository;
    @Mock
    private ICore core;
    private ControllerTempAuto controller;

    @Test
    void temperaturaActualizadaLlegaAlCore() {
        List<TemperatureMessage> tempReadings = new ArrayList<>();
        tempReadings.add(new TemperatureMessage("0", "termo-1", 18.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
        when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

        when(switchClient.consultarEstado(any())).thenReturn(new SwitchStateResponse("switch-1", SwitchState.APAGADO, null));
        SiteStateService estadoService = new SiteStateService(switchClient, tempReadingsRepository);

        estadoService.loadSiteState(new DataSite(
                new Site("site-1", "Casa", 5.0, new Fee(null)),
                List.of(new Room(
                        "hab-1",
                        "Living",
                        21.0,
                        1.5,
                        "termo-1",
                        "termo-1/living/temperatura",
                        "switch-1",
                        "http://localhost/switches/1",
                        19.0,
                        SwitchState.APAGADO
                ))
        ));

        TemperatureMessage tempMsg = new TemperatureMessage(
                "0",
                "termo-1",
                18.0,
                18.0,
                Instant.ofEpochMilli(1792846863503L)
        );

        controller = new ControllerTempAuto(switchClient, core, estadoService);

        when(core.calcularComandos(any(), any())).thenReturn(new CoreAnswer(List.of()));

        controller.start(Instant.now(), 1);
        controller.processTemperature(tempMsg);

        ArgumentCaptor<DataSite> captor = ArgumentCaptor.forClass(DataSite.class);

        verify(core).calcularComandos(any(), captor.capture());

        assertEquals(18.0, captor.getValue().habitaciones().get(0).currentTemperatureC());
    }
}
