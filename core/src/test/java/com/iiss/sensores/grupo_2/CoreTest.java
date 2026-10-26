package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.enums.RushDays;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.modelos.output.CommandSwitch;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreTest {
  private final ICore core = new Core();

    @Test
    void calcularComandos_apagaTodoCuandoHayHorarioPunta() throws Exception {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.51,
                SwitchState.ENCENDIDO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                1.0,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.48,
                SwitchState.APAGADO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T18:00:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
            new CommandSwitch("sw-sim-room1", SwitchState.APAGADO)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2026-10-10T18:00:00Z",
        "2026-10-11T18:00:00Z"
    })
    void calcularComandos_noApagaSwitchesEnFinDeSemanaConTarifaDeDiasHabiles(String fechaHora) {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.51,
                SwitchState.ENCENDIDO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                1.0,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.48,
                SwitchState.APAGADO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

        CoreAnswer respuesta = core.calcularComandos(ZonedDateTime.parse(fechaHora), config);

    assertComandosIguales(respuesta);
  }

    @Test
    void calcularComandos_apagaHabitacionesPorEncimaDeLaTemperaturaEsperada() throws Exception {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                21.62,
                SwitchState.ENCENDIDO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                20.14,
                SwitchState.ENCENDIDO
            ),
            new Room(
                "room3",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room3",
                "ht-sim-room3/status/temperature:0",
                "sw-sim-room3",
                "http://switches:8081/sw-sim-room3",
                19.14,
                SwitchState.ENCENDIDO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
            new CommandSwitch("sw-sim-room1", SwitchState.APAGADO),
            new CommandSwitch("sw-sim-room2", SwitchState.APAGADO)
        );
    }

    @Test
    void calcularComandos_enciendeLaHabitacionConMayorDeficitSiHayPotenciaDisponible() throws Exception {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                21.12,
                SwitchState.APAGADO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                17.00,
                SwitchState.APAGADO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

        assertComandosIguales(respuesta,
                new CommandSwitch("sw-sim-room2", SwitchState.ENCENDIDO)
        );
    }

    @Test
    void calcularComandos_noEnciendeNadaCuandoNoHayPotenciaDisponible() throws Exception {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                20.12,
                SwitchState.APAGADO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                18.00,
                SwitchState.ENCENDIDO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

    @Test
    void calcularComandos_noGeneraAccionCuandoLaTemperaturaEstaDentroDelRangoEsperado() throws Exception {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                23.12,
                SwitchState.APAGADO
            ),
            new Room(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                21.00,
                SwitchState.APAGADO
            )
        );
        DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

    @Test
    void calcularComandos_apagaAntesDeEncenderYSeleccionaVariasHabitacionesHastaElLimiteDePotencia() {
        Site site = new Site(
            "casa",
            "Casa",
            2.0,
            new Fee(new RushHour("17:00",
            "23:00",
            RushDays.HABILES)));
        List<Room> habitaciones = List.of(
            new Room(
                "mayor",
                "Mayor deficit",
                22.0,
                1.2,
                "t2",
                "t2",
                "sw-mayor",
                "http://switches:8081/sw-mayor",
                18.0,
                SwitchState.APAGADO),
            new Room(
                "siguiente",
                "Siguiente deficit",
                21.0,
                0.8,
                "t3",
                "t3",
                "sw-siguiente",
                "http://switches:8081/sw-siguiente",
                18.0,
            SwitchState.APAGADO),
            new Room(
                "ultima",
                "No cabe",
                20.0,
                1.0,
                "t4",
                "t4",
                "sw-ultima",
                "http://switches:8081/sw-ultima",
                18.0,
                SwitchState.APAGADO),
            new Room(
                "sobre",
                "Sobre temperatura",
                21.0,
                1.0,
                "t1",
                "t1",
                "sw-sobre",
                "http://switches:8081/sw-sobre",
                22.0,
                SwitchState.ENCENDIDO)
        );
        DataSite config = new DataSite(site, habitaciones);

        CoreAnswer respuesta = core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

        assertComandosEnOrden(respuesta,
            new CommandSwitch("sw-sobre", SwitchState.APAGADO),
            new CommandSwitch("sw-mayor", SwitchState.ENCENDIDO),
            new CommandSwitch("sw-siguiente", SwitchState.ENCENDIDO)
        );
    }

    @Test
    void calcularComandos_noEmiteComandosCuandoLosSwitchesYaTienenElEstadoObjetivo() {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room(
                "room1",
                "Living",
                21.5,
                1.2,
                "t1",
                "t1",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.0,
                SwitchState.ENCENDIDO),
            new Room(
                "room2",
                "Dormitorio",
                20.0,
                0.8,
                "t2",
                "t2",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.0,
                SwitchState.APAGADO)
        );
        DataSite config = new DataSite(site, habitaciones);

        CoreAnswer respuesta = core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

    assertComandosIguales(respuesta);
  }

    @Test
    void calcularComandos_reanudaControlNormalCuandoTerminaElHorarioPunta() {
        Site site = new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES))
        );
        List<Room> habitaciones = List.of(
            new Room("room1",
            "Living",
            21.5,
            1.2,
            "t1",
            "t1",
            "sw-sim-room1",
            "http://switches:8081/sw-sim-room1",
            19.0,
            SwitchState.APAGADO),
            new Room("room2",
            "Dormitorio",
            20.0,
            0.8,
            "t2",
            "t2",
            "sw-sim-room2",
            "http://switches:8081/sw-sim-room2",
            22.0,
            SwitchState.APAGADO)
        );
        DataSite config = new DataSite(site, habitaciones);
        ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

        CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-sim-room1", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoSitioEsNull() {
    DataSite config = null;
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoHabitacionesEsNull() {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    DataSite config = new DataSite(site, null);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoHabitacionesEsUnaListaVacia() {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    DataSite config = new DataSite(site, new ArrayList<>());
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

    private void assertComandosIguales(CoreAnswer respuesta, CommandSwitch... esperados) {
        assertEquals(esperados.length, respuesta.commands().size(),
                "La respuesta debe devolver la misma cantidad de commands que los esperados");

        for (CommandSwitch esperado : esperados) {
            assertTrue(respuesta.commands().stream().anyMatch(actual ->
                switch(actual) {
                    case CommandSwitch actualSwitch when
                        actualSwitch.idSwitch().equals(esperado.idSwitch()) &&
                        actualSwitch.estado() == esperado.estado() -> true;
                    default -> false;
                }
            ), "No se encontró el comando esperado: " + esperado.idSwitch() + " -> " + esperado.estado());
        }
    }

    private void assertComandosEnOrden(CoreAnswer respuesta, CommandSwitch... esperados) {
        assertEquals(esperados.length, respuesta.commands().size(),
                "La respuesta debe devolver la misma cantidad de commands que los esperados");

        for (int indice = 0; indice < esperados.length; indice++) {
            CommandSwitch esperado = esperados[indice];
            assertTrue(switch (respuesta.commands().get(indice)) {
                case CommandSwitch actualSwitch when
                    actualSwitch.idSwitch().equals(esperado.idSwitch()) &&
                    actualSwitch.estado() == esperado.estado() -> true;
                default -> false;
            }, "El comando en la posición " + indice + " no coincide con el esperado: " +
                    esperado.idSwitch() + " -> " + esperado.estado());
        }
    }
}
