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

/** Comprueba las reglas de decisión del Core para temperaturas, tarifas y potencia. */
class CoreTest {
  private final ICore core = new Core();

  /** Verifica que la hora punta apague los switches que estaban encendidos. */
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

  /** Verifica que una tarifa de días hábiles no apague switches durante el fin de semana. */
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

  /** Verifica que se apaguen las habitaciones que alcanzaron la temperatura objetivo. */
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

  /** Verifica que se priorice la habitación con mayor déficit cuando hay potencia. */
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

  /** Verifica que no se agregue consumo cuando se agotó la potencia disponible. */
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

  /** Verifica que no se generen comandos cuando las temperaturas no requieren cambios. */
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

  /** Verifica que primero se libere potencia y luego se encienda hasta el límite contratado. */
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

  /** Verifica que no se repitan comandos si los switches ya tienen el estado objetivo. */
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

  /** Verifica que el control normal se reanude al terminar la hora punta. */
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

  /** Verifica la respuesta vacía cuando no se proporciona un sitio. */
  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoSitioEsNull() {
    DataSite config = null;
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  /** Verifica la respuesta vacía cuando no se proporciona la lista de habitaciones. */
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

  /** Verifica la respuesta vacía cuando el sitio no contiene habitaciones. */
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
   * Anti-starvation test:
   * Scenario with 5 rooms and limited contracted power (4.0 kW, 2.0 kW per room).
   * Only 2 rooms can be powered simultaneously.
   * Room 1 and Room 2 were previously ON and are now within the thermal inertia band
   * (e.g. 20.8 °C and 20.7 °C with expected 21.0 °C, deficit < 0.5 °C).
   * Rooms 3, 4, 5 are cold (e.g. Room 5 is the coldest, like the grandma's room at 14.0 °C).
   * Verifies that Room 1 and Room 2 yield power (turn OFF) early due to thermal inertia,
   * allowing the coldest rooms (like Room 5 and Room 4) to turn ON without suffering starvation.
   */
  /** Verifica que la banda de inercia libere potencia para evitar postergar habitaciones frías. */
  @Test
  void calcularComandos_liberaPorInerciaParaEvitarInanicionDeHabitacionesMasFrias() {
    Site site =
        new Site("casa-5hab", "Casa de 5 habitaciones", 4.0, new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> habitaciones =
        List.of(
            // Room 1: ON, close to target (20.8 / 21.0 -> deficit 0.2, inside inertia band)
            new Room("r1", "Habitacion 1", 21.0, 2.0, "t1", "t1", "sw1", "http://switches/sw1", 20.8, SwitchState.ENCENDIDO),
            // Room 2: ON, close to target (20.7 / 21.0 -> deficit 0.3, inside inertia band)
            new Room("r2", "Habitacion 2", 21.0, 2.0, "t2", "t2", "sw2", "http://switches/sw2", 20.7, SwitchState.ENCENDIDO),
            // Room 3: OFF, deficit 1.5
            new Room("r3", "Habitacion 3", 20.0, 2.0, "t3", "t3", "sw3", "http://switches/sw3", 18.5, SwitchState.APAGADO),
            // Room 4: OFF, deficit 2.0
            new Room("r4", "Habitacion 4", 20.0, 2.0, "t4", "t4", "sw4", "http://switches/sw4", 18.0, SwitchState.APAGADO),
            // Room 5 (abuela): OFF, huge deficit 7.0 (14 °C / 21 °C)
            new Room("r5", "Habitacion 5 (Abuela)", 21.0, 2.0, "t5", "t5", "sw5", "http://switches/sw5", 14.0, SwitchState.APAGADO)
        );

    DataSite config = new DataSite(site, habitaciones);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    // sw1 and sw2 must turn OFF to yield power due to thermal inertia
    // sw5 (greatest deficit: 7.0 °C) and sw4 (second greatest deficit: 2.0 °C) must turn ON
    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw1", SwitchState.APAGADO),
        new CommandSwitch("sw5", SwitchState.ENCENDIDO)
    );
  }

  /**
   * Inertia test: If a room is within the thermal inertia band but NO other room is awaiting
   * power (or there is ample power available), it remains ON until it reaches expected temperature.
   */
  /** Verifica que la inercia no apague equipos si no compiten por potencia. */
  @Test
  void calcularComandos_noApagaPorInerciaSiNoHayCompetenciaPorPotencia() {
    Site site =
        new Site("casa-sobra-potencia", "Casa con potencia", 10.0, new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> habitaciones =
        List.of(
            // Room 1: ON, close to target (20.8 / 21.0), but power is ample (10 kW)
            new Room("r1", "Habitacion 1", 21.0, 2.0, "t1", "t1", "sw1", "http://switches/sw1", 20.8, SwitchState.ENCENDIDO),
            // Room 2: OFF, deficit 2.0
            new Room("r2", "Habitacion 2", 20.0, 2.0, "t2", "t2", "sw2", "http://switches/sw2", 18.0, SwitchState.APAGADO)
        );

    DataSite config = new DataSite(site, habitaciones);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    CoreAnswer respuesta = core.calcularComandos(fechaHora, config);

    // sw1 stays ON (no command), sw2 turns ON because there is enough power for both
    assertComandosIguales(
        respuesta,
        new CommandSwitch("sw2", SwitchState.ENCENDIDO)
    );
  }

  /**
   * Simulates dynamic temperature evolution across multiple cycles (such as over a 2-hour span).
   * 5 rooms with a 4.0 kW limit (each room 2.0 kW, only 2 rooms ON at a time).
   * Cycle 1: Rooms 1 and 2 start with cold deficit (e.g. 17 °C / 21 °C). They turn ON.
   * Cycle 2: After heating, Rooms 1 and 2 enter their thermal inertia band (20.6 °C and 20.7 °C).
   *          Meanwhile, Room 5 (abuela at 14 °C) and Room 4 (18 °C) need heating.
   *          Core yields power from Rooms 1 and 2 early to heat Room 5 and Room 4.
   * Cycle 3: Rooms 4 and 5 heat up into their comfort band, while Room 3 (18.5 °C) gets its turn.
   * Confirms that across the time window, ALL 5 rooms receive power and avoid starvation.
   */
  /** Simula varios ciclos y verifica la rotación de potencia entre habitaciones. */
  @Test
  void calcularComandos_simulacionDinamicaDosHorasCincoHabitacionesRotanCorrectamente() {
    Site site = new Site("casa-5hab", "Casa 5 hab", 4.0, new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));
    ZonedDateTime t0 = ZonedDateTime.parse("2026-10-07T10:00:00Z");

    // --- CICLO 1: Estado inicial al minuto 0 ---
    List<Room> habsCiclo1 = List.of(
        new Room("r1", "H1", 21.0, 2.0, "t1", "t1", "sw1", "url/1", 16.0, SwitchState.APAGADO), // deficit 5.0
        new Room("r2", "H2", 21.0, 2.0, "t2", "t2", "sw2", "url/2", 17.0, SwitchState.APAGADO), // deficit 4.0
        new Room("r3", "H3", 20.0, 2.0, "t3", "t3", "sw3", "url/3", 18.0, SwitchState.APAGADO), // deficit 2.0
        new Room("r4", "H4", 20.0, 2.0, "t4", "t4", "sw4", "url/4", 17.5, SwitchState.APAGADO), // deficit 2.5
        new Room("r5", "H5 (Abuela)", 21.0, 2.0, "t5", "t5", "sw5", "url/5", 14.0, SwitchState.APAGADO) // deficit 7.0
    );
    CoreAnswer ans1 = core.calcularComandos(t0, new DataSite(site, habsCiclo1));
    // Encienden r5 (deficit 7.0) y r1 (deficit 5.0) -> ocupan los 4.0 kW
    assertComandosIguales(ans1,
        new CommandSwitch("sw5", SwitchState.ENCENDIDO),
        new CommandSwitch("sw1", SwitchState.ENCENDIDO)
    );

    // --- CICLO 2: Minuto 45 (r5 y r1 calentaron; r1 llega a banda de inercia 20.6 °C; r5 a 19.5 °C) ---
    List<Room> habsCiclo2 = List.of(
        new Room("r1", "H1", 21.0, 2.0, "t1", "t1", "sw1", "url/1", 20.6, SwitchState.ENCENDIDO), // deficit 0.4 (en inercia)
        new Room("r2", "H2", 21.0, 2.0, "t2", "t2", "sw2", "url/2", 17.0, SwitchState.APAGADO), // deficit 4.0
        new Room("r3", "H3", 20.0, 2.0, "t3", "t3", "sw3", "url/3", 18.0, SwitchState.APAGADO), // deficit 2.0
        new Room("r4", "H4", 20.0, 2.0, "t4", "t4", "sw4", "url/4", 17.5, SwitchState.APAGADO), // deficit 2.5
        new Room("r5", "H5 (Abuela)", 21.0, 2.0, "t5", "t5", "sw5", "url/5", 19.5, SwitchState.ENCENDIDO) // deficit 1.5
    );
    CoreAnswer ans2 = core.calcularComandos(t0.plusMinutes(45), new DataSite(site, habsCiclo2));
    // r1 cede potencia (apaga) porque r2 tiene deficit 4.0 > 0.4 y no cabe. r2 se enciende. r5 sigue encendido.
    assertComandosIguales(ans2,
        new CommandSwitch("sw1", SwitchState.APAGADO),
        new CommandSwitch("sw2", SwitchState.ENCENDIDO)
    );

    // --- CICLO 3: Minuto 90 (r5 llega a banda de inercia 20.7 °C; r2 llega a 19.8 °C) ---
    List<Room> habsCiclo3 = List.of(
        new Room("r1", "H1", 21.0, 2.0, "t1", "t1", "sw1", "url/1", 20.8, SwitchState.APAGADO), // inercia manteniendo
        new Room("r2", "H2", 21.0, 2.0, "t2", "t2", "sw2", "url/2", 19.8, SwitchState.ENCENDIDO), // deficit 1.2
        new Room("r3", "H3", 20.0, 2.0, "t3", "t3", "sw3", "url/3", 18.0, SwitchState.APAGADO), // deficit 2.0
        new Room("r4", "H4", 20.0, 2.0, "t4", "t4", "sw4", "url/4", 17.5, SwitchState.APAGADO), // deficit 2.5
        new Room("r5", "H5 (Abuela)", 21.0, 2.0, "t5", "t5", "sw5", "url/5", 20.7, SwitchState.ENCENDIDO) // deficit 0.3 (en inercia)
    );
    CoreAnswer ans3 = core.calcularComandos(t0.plusMinutes(90), new DataSite(site, habsCiclo3));
    // r5 cede potencia (apaga) porque r4 tiene deficit 2.5 y r3 tiene deficit 2.0. r4 enciende.
    assertComandosIguales(ans3,
        new CommandSwitch("sw5", SwitchState.APAGADO),
        new CommandSwitch("sw4", SwitchState.ENCENDIDO)
    );
  }

  /** Compara los comandos calculados sin considerar su orden. */
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

  /** Compara los comandos calculados respetando su orden de prioridad. */
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


