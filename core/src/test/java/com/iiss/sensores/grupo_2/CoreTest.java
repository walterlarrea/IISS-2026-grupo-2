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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreTest {
  private final ICore core = new Core();

  @Test
  void calcularComandos_apagaTodoCuandoHayHorarioPunta() throws Exception {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.ENCENDIDO),
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
                SwitchState.APAGADO));
    DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T18:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-sim-room1", SwitchState.APAGADO));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-10-10T18:00:00Z", "2026-10-11T18:00:00Z"})
  void calcularComandos_noApagaSwitchesEnFinDeSemanaConTarifaDeDiasHabiles(String fechaHora) {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.ENCENDIDO),
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
                SwitchState.APAGADO));
    DataSite config = new DataSite(site, habitaciones);

    CoreAnswer respuesta = core.calcularComandos(ZonedDateTime.parse(fechaHora), config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_apagaHabitacionesPorEncimaDeLaTemperaturaEsperada() throws Exception {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.ENCENDIDO),
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
                SwitchState.ENCENDIDO),
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
                SwitchState.ENCENDIDO));
    DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw-sim-room1", SwitchState.APAGADO),
        new CommandSwitch("sw-sim-room2", SwitchState.APAGADO));
  }

  @Test
  void calcularComandos_enciendeLaHabitacionConMayorDeficitSiHayPotenciaDisponible()
      throws Exception {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.APAGADO),
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
                SwitchState.APAGADO));
    DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-sim-room2", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_noEnciendeNadaCuandoNoHayPotenciaDisponible() throws Exception {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.APAGADO),
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
                SwitchState.ENCENDIDO));
    DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_noGeneraAccionCuandoLaTemperaturaEstaDentroDelRangoEsperado()
      throws Exception {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.APAGADO),
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
                SwitchState.APAGADO));
    DataSite config = new DataSite(site, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_apagaAntesDeEncenderYSeleccionaVariasHabitacionesHastaElLimiteDePotencia() {
    Site site =
        new Site("casa", "Casa", 2.0, new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.ENCENDIDO));
    DataSite config = new DataSite(site, habitaciones);

    CoreAnswer respuesta =
        core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

    assertComandosEnOrden(
        respuesta,
        new CommandSwitch("sw-sobre", SwitchState.APAGADO),
        new CommandSwitch("sw-mayor", SwitchState.ENCENDIDO),
        new CommandSwitch("sw-siguiente", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_noEmiteComandosCuandoLosSwitchesYaTienenElEstadoObjetivo() {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.APAGADO));
    DataSite config = new DataSite(site, habitaciones);

    CoreAnswer respuesta =
        core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_reanudaControlNormalCuandoTerminaElHorarioPunta() {
    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    List<Room> habitaciones =
        List.of(
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
                SwitchState.APAGADO),
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
                SwitchState.APAGADO));
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

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  /**
   * Anti-starvation test: Scenario with 5 rooms and limited contracted power (4.0 kW, 2.0 kW per
   * room). Only 2 rooms can be powered simultaneously. Room 1 and Room 2 were previously ON and are
   * now within the thermal inertia band (e.g. 20.8 °C and 20.7 °C with expected 21.0 °C, deficit <
   * 0.5 °C). Rooms 3, 4, 5 are cold (e.g. Room 5 is the coldest, like the grandma's room at 14.0
   * °C). Verifies that Room 1 and Room 2 yield power (turn OFF) early due to thermal inertia,
   * allowing the coldest rooms (like Room 5 and Room 4) to turn ON without suffering starvation.
   */
  @Test
  void calcularComandos_liberaPorInerciaParaEvitarInanicionDeHabitacionesMasFrias() {
    Site site =
        new Site(
            "casa-5hab",
            "Casa de 5 habitaciones",
            4.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> habitaciones =
        List.of(
            // Room 1: ON, close to target (20.95 / 21.0 -> deficit 0.05, inside inertia band)
            new Room(
                "r1",
                "Habitacion 1",
                21.0,
                2.0,
                "t1",
                "t1",
                "sw1",
                "http://switches/sw1",
                20.95,
                SwitchState.ENCENDIDO),
            // Room 2: ON, close to target (20.92 / 21.0 -> deficit 0.08, inside inertia band)
            new Room(
                "r2",
                "Habitacion 2",
                21.0,
                2.0,
                "t2",
                "t2",
                "sw2",
                "http://switches/sw2",
                20.92,
                SwitchState.ENCENDIDO),
            // Room 3: OFF, deficit 1.5
            new Room(
                "r3",
                "Habitacion 3",
                20.0,
                2.0,
                "t3",
                "t3",
                "sw3",
                "http://switches/sw3",
                18.5,
                SwitchState.APAGADO),
            // Room 4: OFF, deficit 2.0
            new Room(
                "r4",
                "Habitacion 4",
                20.0,
                2.0,
                "t4",
                "t4",
                "sw4",
                "http://switches/sw4",
                18.0,
                SwitchState.APAGADO),
            // Room 5 (abuela): OFF, huge deficit 7.0 (14 °C / 21 °C)
            new Room(
                "r5",
                "Habitacion 5 (Abuela)",
                21.0,
                2.0,
                "t5",
                "t5",
                "sw5",
                "http://switches/sw5",
                14.0,
                SwitchState.APAGADO));

    DataSite config = new DataSite(site, habitaciones);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    // sw1 and sw2 must turn OFF to yield power due to thermal inertia
    // sw5 (greatest deficit: 7.0 °C) and sw4 (second greatest deficit: 2.0 °C) must turn ON
    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw1", SwitchState.APAGADO),
        new CommandSwitch("sw5", SwitchState.ENCENDIDO));
  }

  /**
   * Inertia test: If a room is within the thermal inertia band but NO other room is awaiting power
   * (or there is ample power available), it remains ON until it reaches expected temperature.
   */
  @Test
  void calcularComandos_noApagaPorInerciaSiNoHayCompetenciaPorPotencia() {
    Site site =
        new Site(
            "casa-sobra-potencia",
            "Casa con potencia",
            10.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> habitaciones =
        List.of(
            // Room 1: ON, close to target (20.8 / 21.0), but power is ample (10 kW)
            new Room(
                "r1",
                "Habitacion 1",
                21.0,
                2.0,
                "t1",
                "t1",
                "sw1",
                "http://switches/sw1",
                20.8,
                SwitchState.ENCENDIDO),
            // Room 2: OFF, deficit 2.0
            new Room(
                "r2",
                "Habitacion 2",
                20.0,
                2.0,
                "t2",
                "t2",
                "sw2",
                "http://switches/sw2",
                18.0,
                SwitchState.APAGADO));

    DataSite config = new DataSite(site, habitaciones);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    // sw1 stays ON (no command), sw2 turns ON because there is enough power for both
    assertComandosIguales(respuesta, new CommandSwitch("sw2", SwitchState.ENCENDIDO));
  }

  /**
   * Inertia test: a room inside the inertia band must not yield when the power it frees is not
   * enough for the colder room to fit; turning it OFF would only waste heating.
   */
  @Test
  void calcularComandos_noLiberaPorInerciaSiLaPotenciaLiberadaNoAlcanzaParaLaMasFria() {
    Site site =
        new Site(
            "casa-insuficiente",
            "Casa insuficiente",
            4.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> habitaciones =
        List.of(
            // r1: ON, inside the inertia band (20.8 / 21.0), frees only 1.0 kW
            new Room(
                "r1",
                "Habitacion 1",
                21.0,
                1.0,
                "t1",
                "t1",
                "sw1",
                "http://switches/sw1",
                20.8,
                SwitchState.ENCENDIDO),
            // r2: ON, far from target, outside the inertia band
            new Room(
                "r2",
                "Habitacion 2",
                21.0,
                3.0,
                "t2",
                "t2",
                "sw2",
                "http://switches/sw2",
                15.0,
                SwitchState.ENCENDIDO),
            // r3: OFF, coldest, needs 3.0 kW: 1.0 kW from r1 would not be enough
            new Room(
                "r3",
                "Habitacion 3",
                21.0,
                3.0,
                "t3",
                "t3",
                "sw3",
                "http://switches/sw3",
                14.0,
                SwitchState.APAGADO));

    CoreAnswer respuesta =
        core.calcularComandos(
            ZonedDateTime.parse("2026-10-07T12:00:00Z"), new DataSite(site, habitaciones));

    assertTrue(respuesta.commands().isEmpty(), "No debe apagarse r1 si r3 igual no entra");
  }

  /**
   * Simulates dynamic temperature evolution across multiple cycles (such as over a 2-hour span). 5
   * rooms with a 4.0 kW limit (each room 2.0 kW, only 2 rooms ON at a time). Cycle 1: Rooms 1 and 2
   * start with cold deficit (e.g. 17 °C / 21 °C). They turn ON. Cycle 2: After heating, Rooms 1 and
   * 2 enter their thermal inertia band (20.6 °C and 20.7 °C). Meanwhile, Room 5 (abuela at 14 °C)
   * and Room 4 (18 °C) need heating. Core yields power from Rooms 1 and 2 early to heat Room 5 and
   * Room 4. Cycle 3: Rooms 4 and 5 heat up into their comfort band, while Room 3 (18.5 °C) gets its
   * turn. Confirms that across the time window, ALL 5 rooms receive power and avoid starvation.
   */
  @Test
  void calcularComandos_simulacionDinamicaDosHorasCincoHabitacionesRotanCorrectamente() {
    Site site =
        new Site(
            "casa-5hab",
            "Casa 5 hab",
            4.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    ZonedDateTime t0 = ZonedDateTime.parse("2026-10-07T10:00:00Z");

    // --- CICLO 1: Estado inicial al minuto 0 ---
    List<Room> habsCiclo1 =
        List.of(
            new Room(
                "r1",
                "H1",
                21.0,
                2.0,
                "t1",
                "t1",
                "sw1",
                "url/1",
                16.0,
                SwitchState.APAGADO), // deficit 5.0
            new Room(
                "r2",
                "H2",
                21.0,
                2.0,
                "t2",
                "t2",
                "sw2",
                "url/2",
                17.0,
                SwitchState.APAGADO), // deficit 4.0
            new Room(
                "r3",
                "H3",
                20.0,
                2.0,
                "t3",
                "t3",
                "sw3",
                "url/3",
                18.0,
                SwitchState.APAGADO), // deficit 2.0
            new Room(
                "r4",
                "H4",
                20.0,
                2.0,
                "t4",
                "t4",
                "sw4",
                "url/4",
                17.5,
                SwitchState.APAGADO), // deficit 2.5
            new Room(
                "r5",
                "H5 (Abuela)",
                21.0,
                2.0,
                "t5",
                "t5",
                "sw5",
                "url/5",
                14.0,
                SwitchState.APAGADO) // deficit 7.0
            );
    CoreAnswer ans1 = core.calcularComandos(t0, new DataSite(site, habsCiclo1));
    // Encienden r5 (deficit 7.0) y r1 (deficit 5.0) -> ocupan los 4.0 kW
    assertComandosIguales(
        ans1,
        new CommandSwitch("sw5", SwitchState.ENCENDIDO),
        new CommandSwitch("sw1", SwitchState.ENCENDIDO));

    // --- CICLO 2: Minuto 45 (r5 y r1 calentaron; r1 llega a banda de inercia 20.95 °C; r5 a 19.5
    // °C) ---
    List<Room> habsCiclo2 =
        List.of(
            new Room(
                "r1",
                "H1",
                21.0,
                2.0,
                "t1",
                "t1",
                "sw1",
                "url/1",
                20.95,
                SwitchState.ENCENDIDO), // deficit 0.05 (en inercia)
            new Room(
                "r2",
                "H2",
                21.0,
                2.0,
                "t2",
                "t2",
                "sw2",
                "url/2",
                17.0,
                SwitchState.APAGADO), // deficit 4.0
            new Room(
                "r3",
                "H3",
                20.0,
                2.0,
                "t3",
                "t3",
                "sw3",
                "url/3",
                18.0,
                SwitchState.APAGADO), // deficit 2.0
            new Room(
                "r4",
                "H4",
                20.0,
                2.0,
                "t4",
                "t4",
                "sw4",
                "url/4",
                17.5,
                SwitchState.APAGADO), // deficit 2.5
            new Room(
                "r5",
                "H5 (Abuela)",
                21.0,
                2.0,
                "t5",
                "t5",
                "sw5",
                "url/5",
                19.5,
                SwitchState.ENCENDIDO) // deficit 1.5
            );
    CoreAnswer ans2 = core.calcularComandos(t0.plusMinutes(45), new DataSite(site, habsCiclo2));
    // r1 cede potencia (apaga) porque r2 tiene deficit 4.0 > 0.05 y no cabe. r2 se enciende. r5
    // sigue encendido.
    assertComandosIguales(
        ans2,
        new CommandSwitch("sw1", SwitchState.APAGADO),
        new CommandSwitch("sw2", SwitchState.ENCENDIDO));

    // --- CICLO 3: Minuto 90 (r5 llega a banda de inercia 20.95 °C; r2 llega a 19.8 °C) ---
    List<Room> habsCiclo3 =
        List.of(
            new Room(
                "r1",
                "H1",
                21.0,
                2.0,
                "t1",
                "t1",
                "sw1",
                "url/1",
                20.8,
                SwitchState.APAGADO), // inercia manteniendo
            new Room(
                "r2",
                "H2",
                21.0,
                2.0,
                "t2",
                "t2",
                "sw2",
                "url/2",
                19.8,
                SwitchState.ENCENDIDO), // deficit 1.2
            new Room(
                "r3",
                "H3",
                20.0,
                2.0,
                "t3",
                "t3",
                "sw3",
                "url/3",
                18.0,
                SwitchState.APAGADO), // deficit 2.0
            new Room(
                "r4",
                "H4",
                20.0,
                2.0,
                "t4",
                "t4",
                "sw4",
                "url/4",
                17.5,
                SwitchState.APAGADO), // deficit 2.5
            new Room(
                "r5",
                "H5 (Abuela)",
                21.0,
                2.0,
                "t5",
                "t5",
                "sw5",
                "url/5",
                20.95,
                SwitchState.ENCENDIDO) // deficit 0.05 (en inercia)
            );
    CoreAnswer ans3 = core.calcularComandos(t0.plusMinutes(90), new DataSite(site, habsCiclo3));
    // r5 cede potencia (apaga) porque r4 tiene deficit 2.5 y r3 tiene deficit 2.0. r4 enciende.
    assertComandosIguales(
        ans3,
        new CommandSwitch("sw5", SwitchState.APAGADO),
        new CommandSwitch("sw4", SwitchState.ENCENDIDO));
  }

  // ---------------------------------------------------------------------------------------------
  // Prevención de starvation por espera prolongada (usa Room.lastSwitchChange)
  //
  // Los tiempos son deliberadamente extremos (5 minutos vs. 6 horas) para no depender del valor
  // exacto de la espera máxima ni del factor de aging que se elija al configurar el Core.
  // ---------------------------------------------------------------------------------------------

  private static final ZonedDateTime AHORA = ZonedDateTime.parse("2026-10-07T12:00:00Z");
  private static final ZonedDateTime HACE_5_MIN = AHORA.minusMinutes(5);
  private static final ZonedDateTime HACE_6_HORAS = AHORA.minusHours(6);

  /**
   * Starvation por déficit bajo: con potencia para una sola habitación, la de menor déficit quedaba
   * siempre última. Tras 6 h esperando debe pasar por delante de las que acaban de esperar.
   */
  @Test
  void calcularComandos_enciendeLaHabitacionQueLlevaMuchoEsperandoAunqueTengaMenorDeficit() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 2.0, 18.0, SwitchState.APAGADO, HACE_5_MIN), // déficit 3.0
                sala("b", 21.0, 2.0, 18.5, SwitchState.APAGADO, HACE_5_MIN), // déficit 2.5
                sala("c", 20.0, 2.0, 19.6, SwitchState.APAGADO, HACE_6_HORAS))); // déficit 0.4

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-c", SwitchState.ENCENDIDO));
  }

  /**
   * Habitación inalcanzable (ventana abierta, calefactor subdimensionado): lleva horas encendida
   * con un déficit enorme y acapara toda la potencia. Debe ceder ante la que espera hace horas.
   */
  @Test
  void calcularComandos_desplazaALaHabitacionEncendidaHaceHorasSiOtraLlevaMuchoEsperando() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("inalcanzable", 21.0, 2.0, 13.0, SwitchState.ENCENDIDO, HACE_6_HORAS),
                sala("c", 20.0, 2.0, 19.6, SwitchState.APAGADO, HACE_6_HORAS)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw-inalcanzable", SwitchState.APAGADO),
        new CommandSwitch("sw-c", SwitchState.ENCENDIDO));
  }

  /**
   * Tiempo mínimo encendida (anti-flapping): una habitación recién encendida no se desplaza aunque
   * otra lleve horas esperando.
   */
  @Test
  void calcularComandos_noDesplazaAUnaHabitacionRecienEncendida() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("recien-encendida", 21.0, 2.0, 18.0, SwitchState.ENCENDIDO, AHORA),
                sala("c", 20.0, 2.0, 19.6, SwitchState.APAGADO, HACE_6_HORAS)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta);
  }

  /**
   * Starvation por fragmentación de potencia: una habitación de 3 kW nunca cabía porque las chicas
   * (1.5 kW) con mayor déficit se encendían primero y ocupaban el hueco.
   */
  @Test
  void calcularComandos_enciendeLaHabitacionGrandeQueLlevaMuchoEsperandoAntesQueLasChicas() {
    DataSite config =
        new DataSite(
            sitio(3.0),
            List.of(
                sala("grande", 20.0, 3.0, 19.0, SwitchState.APAGADO, HACE_6_HORAS),
                sala("chica1", 21.0, 1.5, 18.0, SwitchState.APAGADO, HACE_5_MIN),
                sala("chica2", 21.0, 1.5, 19.0, SwitchState.APAGADO, HACE_5_MIN)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-grande", SwitchState.ENCENDIDO));
  }

  /**
   * Igual que el caso anterior pero las habitaciones chicas ya están encendidas hace horas: para
   * que la grande quepa hay que liberar potencia de ambas.
   */
  @Test
  void calcularComandos_liberaPotenciaDeVariasHabitacionesParaQueEntreLaQueLlevaMuchoEsperando() {
    DataSite config =
        new DataSite(
            sitio(3.0),
            List.of(
                sala("grande", 20.0, 3.0, 19.0, SwitchState.APAGADO, HACE_6_HORAS),
                sala("chica1", 21.0, 1.5, 18.0, SwitchState.ENCENDIDO, HACE_6_HORAS),
                sala("chica2", 21.0, 1.5, 19.0, SwitchState.ENCENDIDO, HACE_6_HORAS)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw-chica1", SwitchState.APAGADO),
        new CommandSwitch("sw-chica2", SwitchState.APAGADO),
        new CommandSwitch("sw-grande", SwitchState.ENCENDIDO));
  }

  /**
   * Sin información de espera (lastSwitchChange null) el criterio sigue siendo el mayor déficit.
   */
  @Test
  void calcularComandos_priorizaPorDeficitCuandoSeDesconoceElTiempoDeEspera() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 2.0, 18.0, SwitchState.APAGADO, null), // déficit 3.0
                sala("c", 20.0, 2.0, 19.6, SwitchState.APAGADO, null))); // déficit 0.4

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  /** Con esperas cortas y parecidas el criterio sigue siendo el mayor déficit. */
  @Test
  void calcularComandos_priorizaPorDeficitCuandoLasEsperasSonCortas() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 2.0, 18.0, SwitchState.APAGADO, HACE_5_MIN), // déficit 3.0
                sala("c", 20.0, 2.0, 19.6, SwitchState.APAGADO, HACE_5_MIN))); // déficit 0.4

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  /** Una habitación que ya alcanzó su temperatura no necesita calefacción por mucho que espere. */
  @Test
  void calcularComandos_noEnciendeUnaHabitacionEnTemperaturaAunqueLleveMuchoApagada() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 2.0, 18.0, SwitchState.APAGADO, HACE_5_MIN),
                sala("en-temperatura", 20.5, 2.0, 21.0, SwitchState.APAGADO, HACE_6_HORAS)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  /** El horario punta tiene precedencia sobre cualquier garantía de espera máxima. */
  @Test
  void calcularComandos_horarioPuntaApagaTodoAunqueHayaHabitacionesEsperandoMucho() {
    ZonedDateTime horaPunta = ZonedDateTime.parse("2026-10-07T18:00:00Z");
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("encendida", 21.0, 2.0, 18.0, SwitchState.ENCENDIDO, horaPunta.minusHours(6)),
                sala("esperando", 20.0, 2.0, 19.0, SwitchState.APAGADO, horaPunta.minusHours(6))));

    CoreAnswer respuesta = core.calcularComandos(horaPunta, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-encendida", SwitchState.APAGADO));
  }

  /**
   * Simulación de 12 h (ciclos de 10 min) con una sola habitación encendida a la vez (2 kW de 2 kW
   * contratados). A y B se enfrían rápido, por lo que su déficit siempre supera al de C, que casi
   * no pierde calor. Con prioridad solo por déficit, C nunca se enciende. Todas deben recibir
   * potencia.
   */
  @Test
  void calcularComandos_simulacionDe12HorasTodasLasHabitacionesRecibenPotencia() {
    String[] ids = {"a", "b", "c"};
    double[] esperada = {21.0, 21.0, 20.0};
    double[] temperatura = {18.0, 18.2, 19.6};
    double[] perdidaPorCiclo = {0.3, 0.3, 0.01};
    double gananciaEncendida = 0.5;
    SwitchState[] estado = {SwitchState.APAGADO, SwitchState.APAGADO, SwitchState.APAGADO};
    ZonedDateTime[] ultimoCambio = {AHORA, AHORA, AHORA};
    Set<String> encendidasAlgunaVez = new HashSet<>();

    for (int ciclo = 0; ciclo < 72; ciclo++) {
      ZonedDateTime instante = AHORA.plusMinutes(10L * ciclo);
      List<Room> habitaciones = new ArrayList<>();
      for (int i = 0; i < ids.length; i++) {
        habitaciones.add(
            sala(ids[i], esperada[i], 2.0, temperatura[i], estado[i], ultimoCambio[i]));
      }

      CoreAnswer respuesta =
          core.calcularComandos(instante, new DataSite(sitio(2.0), habitaciones));

      for (var comando : respuesta.commands()) {
        CommandSwitch cs = (CommandSwitch) comando;
        for (int i = 0; i < ids.length; i++) {
          if (cs.idSwitch().equals("sw-" + ids[i])) {
            estado[i] = cs.estado();
            ultimoCambio[i] = instante;
            if (cs.estado() == SwitchState.ENCENDIDO) {
              encendidasAlgunaVez.add(ids[i]);
            }
          }
        }
      }
      for (int i = 0; i < ids.length; i++) {
        temperatura[i] +=
            (estado[i] == SwitchState.ENCENDIDO ? gananciaEncendida : 0.0) - perdidaPorCiclo[i];
      }
    }

    assertEquals(
        Set.of("a", "b", "c"),
        encendidasAlgunaVez,
        "Todas las habitaciones deben recibir potencia en 12 h; las que no lo hicieron sufrieron starvation");
  }

  // ---------------------------------------------------------------------------------------------
  // Histéresis para encender: evita el vaivén ON/OFF alrededor de la temperatura objetivo
  // (el Core por defecto usa 0.3 °C)
  // ---------------------------------------------------------------------------------------------

  @Test
  void calcularComandos_noEnciendeUnaHabitacionApagadaConDeficitMenorALaHisteresis() {
    DataSite config =
        new DataSite(
            sitio(3.7),
            List.of(sala("a", 21.5, 1.2, 21.3, SwitchState.APAGADO, HACE_5_MIN))); // déficit 0.2

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_enciendeUnaHabitacionApagadaCuandoElDeficitSuperaLaHisteresis() {
    DataSite config =
        new DataSite(
            sitio(3.7),
            List.of(sala("a", 21.5, 1.2, 21.0, SwitchState.APAGADO, HACE_5_MIN))); // déficit 0.5

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_mantieneEncendidaUnaHabitacionDentroDeLaHisteresisHastaElObjetivo() {
    DataSite config =
        new DataSite(
            sitio(3.7),
            List.of(sala("a", 21.5, 1.2, 21.3, SwitchState.ENCENDIDO, HACE_6_HORAS))); // déficit 0.2

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_apagaAlLlegarAlObjetivoYNoVuelveAEncenderHastaBajarLaHisteresis() {
    CoreAnswer alLlegar =
        core.calcularComandos(
            AHORA,
            new DataSite(
                sitio(3.7),
                List.of(sala("a", 21.5, 1.2, 21.5, SwitchState.ENCENDIDO, HACE_6_HORAS))));
    CoreAnswer apenasPorDebajo =
        core.calcularComandos(
            AHORA,
            new DataSite(
                sitio(3.7),
                List.of(sala("a", 21.5, 1.2, 21.4, SwitchState.APAGADO, AHORA))));
    CoreAnswer bajoLaHisteresis =
        core.calcularComandos(
            AHORA,
            new DataSite(
                sitio(3.7),
                List.of(sala("a", 21.5, 1.2, 21.1, SwitchState.APAGADO, AHORA))));

    assertComandosIguales(alLlegar, new CommandSwitch("sw-a", SwitchState.APAGADO));
    assertComandosIguales(apenasPorDebajo);
    assertComandosIguales(bajoLaHisteresis, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  /** Una habitación en la zona muerta no necesita calefacción, por mucho que lleve esperando. */
  @Test
  void calcularComandos_noPromueveALaHabitacionApagadaEnLaZonaMuertaAunqueLleveMuchoEsperando() {
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 2.0, 18.0, SwitchState.APAGADO, HACE_5_MIN),
                sala("zona-muerta", 20.0, 2.0, 19.9, SwitchState.APAGADO, HACE_6_HORAS)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_conHisteresisCeroEnciendeConCualquierDeficit() {
    ICore sinHisteresis =
        CoreFactory.getCore(
            Core.DEFAULT_THERMAL_INERTIA_BAND_C,
            Core.DEFAULT_MAX_WAIT,
            Core.DEFAULT_MIN_ON_TIME,
            0.0);
    DataSite config =
        new DataSite(
            sitio(3.7),
            List.of(sala("a", 21.5, 1.2, 21.49, SwitchState.APAGADO, HACE_5_MIN)));

    CoreAnswer respuesta = sinHisteresis.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-a", SwitchState.ENCENDIDO));
  }

  @Test
  void calcularComandos_trasLaPuntaLaEsperaSeCuentaDesdeElFinDeLaPunta() {
    // La punta terminó a las 23:00; a las 23:30 la sala "a" lleva 3 h apagada pero solo 30 min
    // sin punta: no debe quitarle el turno a "b", encendida hace 30 min
    ZonedDateTime ahora = ZonedDateTime.parse("2026-10-07T23:30:00Z");
    DataSite config =
        new DataSite(
            sitio(1.0),
            List.of(
                sala("a", 21.0, 1.0, 19.0, SwitchState.APAGADO, ahora.minusHours(3)),
                sala("b", 21.0, 1.0, 20.5, SwitchState.ENCENDIDO, ahora.minusMinutes(30))));

    CoreAnswer respuesta = core.calcularComandos(ahora, config);

    assertTrue(respuesta.commands().isEmpty());
  }

  @Test
  void calcularComandos_siLasEncendidasSuperanLaPotenciaContratadaApagaLasDeMenorDeficit() {
    // 3 kW encendidos con 2.0 kW contratados: apaga la menos fría aunque lleve 5 min encendida
    DataSite config =
        new DataSite(
            sitio(2.0),
            List.of(
                sala("a", 21.0, 1.0, 20.0, SwitchState.ENCENDIDO, HACE_5_MIN),
                sala("b", 21.0, 1.0, 20.9, SwitchState.ENCENDIDO, HACE_5_MIN),
                sala("c", 21.0, 1.0, 20.5, SwitchState.ENCENDIDO, HACE_5_MIN)));

    CoreAnswer respuesta = core.calcularComandos(AHORA, config);

    assertComandosIguales(respuesta, new CommandSwitch("sw-b", SwitchState.APAGADO));
  }

  /**
   * Una sola habitación que se enfría 0.02 °C/min y se calienta 0.05 °C/min encendida, durante 8
   * h. Sin histéresis el switch cambia de estado en casi cada ciclo; con la histéresis por defecto
   * lo hace una vez cada varios minutos.
   */
  @Test
  void calcularComandos_laHisteresisReduceElVaivenDelSwitchEnUnaSimulacionDe8Horas() {
    ICore sinHisteresis =
        CoreFactory.getCore(
            Core.DEFAULT_THERMAL_INERTIA_BAND_C,
            Core.DEFAULT_MAX_WAIT,
            Core.DEFAULT_MIN_ON_TIME,
            0.0);

    int conHisteresis = contarCambiosDeSwitchEnOchoHoras(core);
    int sinHisteresisCambios = contarCambiosDeSwitchEnOchoHoras(sinHisteresis);

    assertTrue(
        sinHisteresisCambios > 150,
        "Sin histéresis se espera vaivén, hubo " + sinHisteresisCambios + " cambios");
    assertTrue(
        conHisteresis <= 50, "Con histéresis se esperan pocos cambios, hubo " + conHisteresis);
  }

  private static int contarCambiosDeSwitchEnOchoHoras(ICore core) {
    Site sitioSinPunta = new Site("casa", "Casa", 3.7, new Fee(null));
    double temperatura = 20.0;
    SwitchState estado = SwitchState.APAGADO;
    ZonedDateTime ultimoCambio = AHORA;
    int cambios = 0;
    for (int minuto = 0; minuto < 8 * 60; minuto++) {
      ZonedDateTime instante = AHORA.plusMinutes(minuto);
      if (estado == SwitchState.APAGADO && temperatura >= 21.5) {
        ultimoCambio = instante;
      }
      double lectura = Math.round(temperatura * 100) / 100.0;
      CoreAnswer respuesta =
          core.calcularComandos(
              instante,
              new DataSite(
                  sitioSinPunta, List.of(sala("a", 21.5, 1.2, lectura, estado, ultimoCambio))));
      for (var comando : respuesta.commands()) {
        CommandSwitch cs = (CommandSwitch) comando;
        if (cs.estado() != estado) {
          estado = cs.estado();
          ultimoCambio = instante;
          cambios++;
        }
      }
      temperatura += (estado == SwitchState.ENCENDIDO ? 0.05 : 0.0) - 0.02;
    }
    return cambios;
  }

  private static Site sitio(double potenciaContratadaKW) {
    return new Site(
        "casa",
        "Casa",
        potenciaContratadaKW,
        new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
  }

  private static Room sala(
      String id,
      double temperaturaEsperada,
      double potenciaKW,
      double temperaturaActual,
      SwitchState estado,
      ZonedDateTime ultimoCambio) {
    return new Room(
        id,
        id,
        temperaturaEsperada,
        potenciaKW,
        "t-" + id,
        "t-" + id,
        "sw-" + id,
        "http://switches:8081/sw-" + id,
        temperaturaActual,
        estado,
        ultimoCambio);
  }

  private void assertComandosIguales(CoreAnswer respuesta, CommandSwitch... esperados) {
    assertEquals(
        esperados.length,
        respuesta.commands().size(),
        "La respuesta debe devolver la misma cantidad de commands que los esperados");

    for (CommandSwitch esperado : esperados) {
      assertTrue(
          respuesta.commands().stream()
              .anyMatch(
                  actual ->
                      switch (actual) {
                        case CommandSwitch actualSwitch
                            when actualSwitch.idSwitch().equals(esperado.idSwitch())
                                && actualSwitch.estado() == esperado.estado() ->
                            true;
                        default -> false;
                      }),
          "No se encontró el comando esperado: "
              + esperado.idSwitch()
              + " -> "
              + esperado.estado());
    }
  }

  private void assertComandosEnOrden(CoreAnswer respuesta, CommandSwitch... esperados) {
    assertEquals(
        esperados.length,
        respuesta.commands().size(),
        "La respuesta debe devolver la misma cantidad de commands que los esperados");

    for (int indice = 0; indice < esperados.length; indice++) {
      CommandSwitch esperado = esperados[indice];
      assertTrue(
          switch (respuesta.commands().get(indice)) {
            case CommandSwitch actualSwitch
                when actualSwitch.idSwitch().equals(esperado.idSwitch())
                    && actualSwitch.estado() == esperado.estado() ->
                true;
            default -> false;
          },
          "El comando en la posición "
              + indice
              + " no coincide con el esperado: "
              + esperado.idSwitch()
              + " -> "
              + esperado.estado());
    }
  }
}
