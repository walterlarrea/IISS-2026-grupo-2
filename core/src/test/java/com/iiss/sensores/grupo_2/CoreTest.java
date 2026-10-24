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
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.51,
                EstadoSwitch.ENCENDIDO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                1.0,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.48,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T18:00:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-10-10T18:00:00Z", "2026-10-11T18:00:00Z"})
  void calcularComandos_noApagaSwitchesEnFinDeSemanaConTarifaDeDiasHabiles(String fechaHora) {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.51,
                EstadoSwitch.ENCENDIDO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                1.0,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.48,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    RespuestaCore respuesta = core.calcularComandos(ZonedDateTime.parse(fechaHora), config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_apagaHabitacionesPorEncimaDeLaTemperaturaEsperada() throws Exception {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                21.62,
                EstadoSwitch.ENCENDIDO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                20.14,
                EstadoSwitch.ENCENDIDO),
            new Habitacion(
                "room3",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room3",
                "ht-sim-room3/status/temperature:0",
                "sw-sim-room3",
                "http://switches:8081/sw-sim-room3",
                19.14,
                EstadoSwitch.ENCENDIDO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(
        respuesta,
        new ComandoSwitch("sw-sim-room1", EstadoSwitch.APAGADO),
        new ComandoSwitch("sw-sim-room2", EstadoSwitch.APAGADO));
  }

  @Test
  void calcularComandos_enciendeLaHabitacionConMayorDeficitSiHayPotenciaDisponible()
      throws Exception {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                21.12,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                17.00,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new ComandoSwitch("sw-sim-room2", EstadoSwitch.ENCENDIDO));
  }

  @Test
  void calcularComandos_noEnciendeNadaCuandoNoHayPotenciaDisponible() throws Exception {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            1.5,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                20.12,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                18.00,
                EstadoSwitch.ENCENDIDO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_noGeneraAccionCuandoLaTemperaturaEstaDentroDelRangoEsperado()
      throws Exception {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                23.12,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "room2",
                "Dormitorio principal",
                20.0,
                0.8,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                21.00,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T12:00:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_apagaAntesDeEncenderYSeleccionaVariasHabitacionesHastaElLimiteDePotencia() {
    Sitio sitio =
        new Sitio("casa", "Casa", 2.0, new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "mayor",
                "Mayor deficit",
                22.0,
                1.2,
                "t2",
                "t2",
                "sw-mayor",
                "http://switches:8081/sw-mayor",
                18.0,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "siguiente",
                "Siguiente deficit",
                21.0,
                0.8,
                "t3",
                "t3",
                "sw-siguiente",
                "http://switches:8081/sw-siguiente",
                18.0,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "ultima",
                "No cabe",
                20.0,
                1.0,
                "t4",
                "t4",
                "sw-ultima",
                "http://switches:8081/sw-ultima",
                18.0,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "sobre",
                "Sobre temperatura",
                21.0,
                1.0,
                "t1",
                "t1",
                "sw-sobre",
                "http://switches:8081/sw-sobre",
                22.0,
                EstadoSwitch.ENCENDIDO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    RespuestaCore respuesta =
        core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

    assertComandosEnOrden(
        respuesta,
        new ComandoSwitch("sw-sobre", EstadoSwitch.APAGADO),
        new ComandoSwitch("sw-mayor", EstadoSwitch.ENCENDIDO),
        new ComandoSwitch("sw-siguiente", EstadoSwitch.ENCENDIDO));
  }

  @Test
  void calcularComandos_noEmiteComandosCuandoLosSwitchesYaTienenElEstadoObjetivo() {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "t1",
                "t1",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.0,
                EstadoSwitch.ENCENDIDO),
            new Habitacion(
                "room2",
                "Dormitorio",
                20.0,
                0.8,
                "t2",
                "t2",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.0,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);

    RespuestaCore respuesta =
        core.calcularComandos(ZonedDateTime.parse("2026-10-07T12:00:00Z"), config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_reanudaControlNormalCuandoTerminaElHorarioPunta() {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    List<Habitacion> habitaciones =
        List.of(
            new Habitacion(
                "room1",
                "Living",
                21.5,
                1.2,
                "t1",
                "t1",
                "sw-sim-room1",
                "http://switches:8081/sw-sim-room1",
                19.0,
                EstadoSwitch.APAGADO),
            new Habitacion(
                "room2",
                "Dormitorio",
                20.0,
                0.8,
                "t2",
                "t2",
                "sw-sim-room2",
                "http://switches:8081/sw-sim-room2",
                22.0,
                EstadoSwitch.APAGADO));
    DataSitio config = new DataSitio(sitio, habitaciones);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta, new ComandoSwitch("sw-sim-room1", EstadoSwitch.ENCENDIDO));
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoSitioEsNull() {
    DataSitio config = null;
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoHabitacionesEsNull() {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    DataSitio config = new DataSitio(sitio, null);
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  @Test
  void calcularComandos_devuelveUnaListaVaciaCuandoHabitacionesEsUnaListaVacia() {
    Sitio sitio =
        new Sitio(
            "casa-rodriguez",
            "Casa Rodríguez",
            3.7,
            new Tarifa(new Punta("17:00", "23:00", DiasPunta.HABILES)));
    DataSitio config = new DataSitio(sitio, new ArrayList<>());
    ZonedDateTime fechaHora = ZonedDateTime.parse("2026-10-07T23:01:00Z");

    RespuestaCore respuesta = core.calcularComandos(fechaHora, config);

    assertComandosIguales(respuesta);
  }

  private void assertComandosIguales(RespuestaCore respuesta, ComandoSwitch... esperados) {
    assertEquals(
        esperados.length,
        respuesta.comandos().size(),
        "La respuesta debe devolver la misma cantidad de comandos que los esperados");

    for (ComandoSwitch esperado : esperados) {
      assertTrue(
          respuesta.comandos().stream()
              .anyMatch(
                  actual ->
                      switch (actual) {
                        case ComandoSwitch actualSwitch
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

  private void assertComandosEnOrden(RespuestaCore respuesta, ComandoSwitch... esperados) {
    assertEquals(
        esperados.length,
        respuesta.comandos().size(),
        "La respuesta debe devolver la misma cantidad de comandos que los esperados");

    for (int indice = 0; indice < esperados.length; indice++) {
      ComandoSwitch esperado = esperados[indice];
      assertTrue(
          switch (respuesta.comandos().get(indice)) {
            case ComandoSwitch actualSwitch
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
