package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.service.EstadoSitioService;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Sitio;
import com.iiss.sensores.grupo_2.modelos.input.Tarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EstadoSitioServiceTest {
    private EstadoSitioService servicio;

    @BeforeEach
    void setUp() {
        servicio = new EstadoSitioService();
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
        DataSitio actualizado = servicio.actualizarTemperatura("termo-1", 18.5);
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
        servicio.actualizarEstadoSwitch("switch-1", EstadoSwitch.ENCENDIDO);
        Habitacion habitacion = servicio.obtenerEstado().habitaciones().get(0);

        assertEquals(EstadoSwitch.ENCENDIDO, habitacion.estadoActualSwitch());
        assertEquals(19.0, habitacion.temperaturaActual());
    }
}
