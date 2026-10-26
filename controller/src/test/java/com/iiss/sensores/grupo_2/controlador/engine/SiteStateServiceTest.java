package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Fee;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.modelos.input.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class SiteStateServiceTest {
    private SiteStateService siteStateService;
    private SwitchClient switchClient;
    private TemperatureReadingRepository tempReadingsRepository;

    @BeforeEach
    void setUp() {
        switchClient = mock(SwitchClient.class);
        tempReadingsRepository = mock(TemperatureReadingRepository.class);

        when(switchClient.consultarEstado(any())).thenReturn(new SwitchStateResponse("switch-1", SwitchState.APAGADO, null));
        List<TemperatureMessage> tempReadings = new ArrayList<>();
        tempReadings.add(new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
        when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

        siteStateService = new SiteStateService(switchClient, tempReadingsRepository);
        Site site = new Site("site-1", "Casa", 5.0, new Fee(null));

        Room room = new Room(
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
        );

        siteStateService.loadSiteState(new DataSite(site, List.of(room)));
    }

    @Test
    void actualizarTemperaturaConservaElRestoDelEstado() {
        DataSite actualizado = siteStateService.updateRoomTemperature("termo-1", 18.5);
        Room room = actualizado.habitaciones().get(0);

        assertEquals(18.5, room.currentTemperatureC());
        assertEquals(21.0, room.expectedTemperature());
        assertEquals("switch-1", room.idSwitch());
        assertEquals(SwitchState.APAGADO, room.currentSwitchState());
    }

    @Test
    void actualizarTemperaturaDeTermostatoInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> siteStateService.updateRoomTemperature("inexistente", 18.5));
    }

    @Test
    void actualizarEstadoSwitchConservaLaTemperatura() {
        siteStateService.getSiteState();
        siteStateService.updateSwitchState("switch-1", SwitchState.ENCENDIDO);
        Room room = siteStateService.getSiteState().habitaciones().get(0);

        assertEquals(SwitchState.ENCENDIDO, room.currentSwitchState());
        assertEquals(19.0, room.currentTemperatureC());
    }

    @Test
    void cargarEstadoConsultaYMapeaElEstadoDeCadaSwitch() {
        clearInvocations(switchClient);

        List<TemperatureMessage> tempReadings = new ArrayList<>();
        tempReadings.add(new TemperatureMessage("0", "termo-2", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
        tempReadings.add(new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
        when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

        when(switchClient.consultarEstado(any()))
                .thenReturn(
                        new SwitchStateResponse("switch-1", SwitchState.ENCENDIDO, null),
                        new SwitchStateResponse("switch-2", SwitchState.APAGADO, null)
                );
        Room segundaRoom = new Room(
                "hab-2",
                "Dormitorio",
                20.0,
                1.0,
                "termo-2",
                "casa/dormitorio/temperatura",
                "switch-2",
                "http://localhost",
                Double.NaN,
                SwitchState.APAGADO
        );

        Room primeraRoom = new Room(
                "hab-1",
                "Living",
                21.0,
                1.5,
                "termo-1",
                "casa/living/temperatura",
                "switch-1",
                "http://localhost/switches/1",
                19.0,
                SwitchState.APAGADO
        );
        DataSite nuevoEstado = new DataSite(
                new Site("site-2", "Casa", 5.0, new Fee(null)),
                List.of(primeraRoom, segundaRoom)
        );
        siteStateService.loadSiteState(nuevoEstado);

        verify(switchClient, never()).consultarEstado(any());

        List<Room> habitaciones = siteStateService.getSiteState().habitaciones();
        assertEquals(SwitchState.ENCENDIDO, habitaciones.get(0).currentSwitchState());
        assertEquals(SwitchState.APAGADO, habitaciones.get(1).currentSwitchState());
        verify(switchClient, times(2)).consultarEstado(any());
    }

    @Test
    void consultarEstadoDeSitioLimpiaLaMarcaDeEstadoSucio() {
        clearInvocations(switchClient);

        siteStateService.getSiteState();
        siteStateService.getSiteState();

        verify(switchClient, times(1)).consultarEstado(any());
    }
}
