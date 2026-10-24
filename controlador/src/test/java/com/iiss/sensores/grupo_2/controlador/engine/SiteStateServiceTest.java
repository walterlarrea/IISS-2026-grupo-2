package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchEstadoResponse;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Sitio;
import com.iiss.sensores.grupo_2.modelos.input.Tarifa;
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

        when(switchClient.consultarEstado(any())).thenReturn(new SwitchEstadoResponse("switch-1", EstadoSwitch.APAGADO, null));
        List<TemperatureMessage> tempReadings = new ArrayList<>();
        tempReadings.add(new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
        when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

        siteStateService = new SiteStateService(switchClient, tempReadingsRepository);
        Sitio sitio = new Sitio("sitio-1", "Casa", 5.0, new Tarifa(null));

        Habitacion habitacion = new Habitacion(
                "hab-1",
                "Living",
                21.0,
                1.5,
                "termo-1",
                "termo-1/living/temperatura",
                "switch-1",
                "http://localhost/switches/1",
                19.0,
                EstadoSwitch.APAGADO
        );

        siteStateService.loadSiteState(new DataSitio(sitio, List.of(habitacion)));
    }

    @Test
    void actualizarTemperaturaConservaElRestoDelEstado() {
        DataSitio actualizado = siteStateService.updateRoomTemperature("termo-1", 18.5);
        Habitacion habitacion = actualizado.habitaciones().get(0);

        assertEquals(18.5, habitacion.currentTemperatureC());
        assertEquals(21.0, habitacion.expectedTemeprature());
        assertEquals("switch-1", habitacion.idSwitch());
        assertEquals(EstadoSwitch.APAGADO, habitacion.currentSwitchState());
    }

    @Test
    void actualizarTemperaturaDeTermostatoInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> siteStateService.updateRoomTemperature("inexistente", 18.5));
    }

    @Test
    void actualizarEstadoSwitchConservaLaTemperatura() {
        siteStateService.getSiteState();
        siteStateService.updateSwitchState("switch-1", EstadoSwitch.ENCENDIDO);
        Habitacion habitacion = siteStateService.getSiteState().habitaciones().get(0);

        assertEquals(EstadoSwitch.ENCENDIDO, habitacion.currentSwitchState());
        assertEquals(19.0, habitacion.currentTemperatureC());
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
                        new SwitchEstadoResponse("switch-1", EstadoSwitch.ENCENDIDO, null),
                        new SwitchEstadoResponse("switch-2", EstadoSwitch.APAGADO, null)
                );
        Habitacion segundaHabitacion = new Habitacion(
                "hab-2",
                "Dormitorio",
                20.0,
                1.0,
                "termo-2",
                "casa/dormitorio/temperatura",
                "switch-2",
                "http://localhost",
                Double.NaN,
                EstadoSwitch.APAGADO
        );

        Habitacion primeraHabitacion = new Habitacion(
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
        );
        DataSitio nuevoEstado = new DataSitio(
                new Sitio("sitio-2", "Casa", 5.0, new Tarifa(null)),
                List.of(primeraHabitacion, segundaHabitacion)
        );
        siteStateService.loadSiteState(nuevoEstado);

        verify(switchClient, never()).consultarEstado(any());

        List<Habitacion> habitaciones = siteStateService.getSiteState().habitaciones();
        assertEquals(EstadoSwitch.ENCENDIDO, habitaciones.get(0).currentSwitchState());
        assertEquals(EstadoSwitch.APAGADO, habitaciones.get(1).currentSwitchState());
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
