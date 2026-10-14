package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchEstadoResponse;
import com.iiss.sensores.grupo_2.controlador.engine.service.EstadoSitioService;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Sitio;
import com.iiss.sensores.grupo_2.modelos.input.Tarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class EstadoSitioServiceTest {
    private EstadoSitioService servicio;
    private SwitchClient switchClient;

    @BeforeEach
    void setUp() {
        switchClient = mock(SwitchClient.class);
        when(switchClient.consultarEstado(any())).thenReturn(new SwitchEstadoResponse("switch-1", EstadoSwitch.APAGADO, null));
        servicio = new EstadoSitioService(switchClient);
        Sitio sitio = new Sitio("sitio-1", "Casa", 5.0, new Tarifa(null));

        Habitacion habitacion = new Habitacion(
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

        servicio.cargarEstado(new DataSitio(sitio, List.of(habitacion)));
    }

    @Test
    void actualizarTemperaturaConservaElRestoDelEstado() {
        DataSitio actualizado = servicio.actualizarTemperatura("casa/living/temperatura", 18.5);
        Habitacion habitacion = actualizado.habitaciones().get(0);

        assertEquals(18.5, habitacion.temperaturaActual());
        assertEquals(21.0, habitacion.temperaturaEsperada());
        assertEquals("switch-1", habitacion.idSwitch());
        assertEquals(EstadoSwitch.APAGADO, habitacion.estadoActualSwitch());
    }

    @Test
    void actualizarTemperaturaDeTermostatoInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarTemperatura("inexistente", 18.5));
    }

    @Test
    void actualizarEstadoSwitchConservaLaTemperatura() {
        servicio.obtenerEstado();
        servicio.actualizarEstadoSwitch("switch-1", EstadoSwitch.ENCENDIDO);
        Habitacion habitacion = servicio.obtenerEstado().habitaciones().get(0);

        assertEquals(EstadoSwitch.ENCENDIDO, habitacion.estadoActualSwitch());
        assertEquals(19.0, habitacion.temperaturaActual());
    }

    @Test
    void cargarEstadoConsultaYMapeaElEstadoDeCadaSwitch() {
        clearInvocations(switchClient);
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
        servicio.cargarEstado(nuevoEstado);

        verify(switchClient, never()).consultarEstado(any());

        List<Habitacion> habitaciones = servicio.obtenerEstado().habitaciones();
        assertEquals(EstadoSwitch.ENCENDIDO, habitaciones.get(0).estadoActualSwitch());
        assertEquals(EstadoSwitch.APAGADO, habitaciones.get(1).estadoActualSwitch());
        verify(switchClient, times(2)).consultarEstado(any());
    }

    @Test
    void consultarEstadoDeSitioLimpiaLaMarcaDeEstadoSucio() {
        clearInvocations(switchClient);

        servicio.obtenerEstado();
        servicio.obtenerEstado();

        verify(switchClient, times(1)).consultarEstado(any());
    }
}
