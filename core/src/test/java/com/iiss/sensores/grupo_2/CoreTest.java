package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.DiasPunta;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Punta;
import com.iiss.sensores.grupo_2.modelos.input.Sitio;
import com.iiss.sensores.grupo_2.modelos.input.Tarifa;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreTest {
    private final ICore core = new Core();

    @Test
    void calcularComandos_apagaTodoCuandoHayHorarioPunta() throws Exception {
        DataSitio config = crearConfig(3.7, 23.51, 22.48, 0.8);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T18:00:00Z");

        RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO),
                new ComandoSwitch("sw-sim-room2", EstadoSwitch.APAGADO)
        );
    }

    @Test
    void calcularComandos_apagaHabitacionesPorEncimaDeLaTemperaturaEsperada() throws Exception {
        DataSitio config = crearConfig(3.7, 21.12, 20.14, 0.8);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new ComandoSwitch("sw-sim-room1", EstadoSwitch.ENCENDIDO),
                new ComandoSwitch("sw-sim-room2", EstadoSwitch.APAGADO)
        );
    }

    @Test
    void calcularComandos_enciendeLaHabitacionConMayorDeficitSiHayPotenciaDisponible() throws Exception {
        DataSitio config = crearConfig(1.5, 21.12, 17.00, 0.8);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO),
                new ComandoSwitch("sw-sim-room2", EstadoSwitch.ENCENDIDO)
        );
    }

    @Test
    void calcularComandos_noEnciendeNadaCuandoNoHayPotenciaDisponible() throws Exception {
        DataSitio config = crearConfig(1.0, 23.12, 21.00, 1.2);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO),
                new ComandoSwitch("sw-sim-room2", EstadoSwitch.APAGADO)
        );
    }

    @Test
    void calcularComandos_noGeneraAccionCuandoLaTemperaturaEstaDentroDelRangoEsperado() throws Exception {
        DataSitio config = crearConfig(3.7, 23.12, 21.00, 0.8);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO),
                new ComandoSwitch("sw-sim-room2", EstadoSwitch.APAGADO)
        );
    }

    private void assertComandosIguales(RespuestaCore respuesta, ComandoSwitch... esperados) {
        assertEquals(esperados.length, respuesta.comandos().size(),
                "La respuesta debe devolver la misma cantidad de comandos que los esperados");

        for (ComandoSwitch esperado : esperados) {
            assertTrue(respuesta.comandos().stream().anyMatch(actual ->
                    actual instanceof ComandoSwitch actualSwitch
                            && actualSwitch.idSwitch().equals(esperado.idSwitch())
                            && actualSwitch.estado() == esperado.estado()
            ), "No se encontró el comando esperado: " + esperado.idSwitch() + " -> " + esperado.estado());
        }
    }

    private DataSitio crearConfig(
            double potenciaContratadaKW,
            double temperaturaRoom1,
            double temperaturaRoom2,
            double potenciaRoom2KW
    ) {
        Sitio sitio = new Sitio(
                "casa-rodriguez",
                "Casa Rodríguez",
                potenciaContratadaKW,
                new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES))
        );
        List<Habitacion> habitaciones = List.of(
                new Habitacion(
                        "room1", "Living", 21.5, 1.2,
                        "ht-sim-room1", "ht-sim-room1/status/temperature:0",
                        "sw-sim-room1", "http://switches:8081",
                        temperaturaRoom1, EstadoSwitch.APAGADO
                ),
                new Habitacion(
                        "room2", "Dormitorio principal", 20.0, potenciaRoom2KW,
                        "ht-sim-room2", "ht-sim-room2/status/temperature:0",
                        "sw-sim-room2", "http://switches:8081",
                        temperaturaRoom2, EstadoSwitch.APAGADO
                )
        );
        return new DataSitio(sitio, habitaciones);
    }
}
