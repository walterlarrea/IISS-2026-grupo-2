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
import java.time.ZonedDateTime;
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

    when(switchClient.consultarEstado(any()))
        .thenReturn(new SwitchStateResponse("switch-1", SwitchState.APAGADO, null));
    List<TemperatureMessage> tempReadings = new ArrayList<>();
    tempReadings.add(
        new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
    when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

    siteStateService = new SiteStateService(switchClient, tempReadingsRepository);
    Site site = new Site("site-1", "Casa", 5.0, new Fee(null));

    Room room =
        new Room(
            "hab-1",
            "Living",
            21.0,
            1.5,
            "termo-1",
            "termo-1/living/temperatura",
            "switch-1",
            "http://localhost/switches/1",
            19.0,
            SwitchState.APAGADO);

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
    assertThrows(
        IllegalArgumentException.class,
        () -> siteStateService.updateRoomTemperature("inexistente", 18.5));
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
  void sellaConLaHoraActualLasHabitacionesSinInicioDeSituacionConocido() {
    ZonedDateTime ahora = ZonedDateTime.parse("2026-10-24T21:00:00Z");

    Room room = siteStateService.getSiteState(ahora).habitaciones().get(0);

    assertEquals(ahora, room.lastSwitchChange());
  }

  @Test
  void noPisaElInicioDeSituacionDeUnaHabitacionQueSigueEsperando() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    siteStateService.getSiteState(inicio);

    Room room = siteStateService.getSiteState(inicio.plusHours(2)).habitaciones().get(0);

    assertEquals(inicio, room.lastSwitchChange());
  }

  @Test
  void reiniciaElRelojDeEsperaDeUnaHabitacionApagadaQueYaAlcanzoSuTemperatura() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    siteStateService.getSiteState(inicio);
    siteStateService.updateRoomTemperature("termo-1", 21.5);

    Room room = siteStateService.getSiteState(inicio.plusHours(2)).habitaciones().get(0);

    assertEquals(inicio.plusHours(2), room.lastSwitchChange());
  }

  @Test
  void actualizarEstadoSwitchRegistraElMomentoDelCambio() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    ZonedDateTime cambio = inicio.plusMinutes(30);
    siteStateService.getSiteState(inicio);

    siteStateService.updateSwitchState("switch-1", SwitchState.ENCENDIDO, cambio);

    Room room = siteStateService.getSiteState(cambio.plusMinutes(5)).habitaciones().get(0);
    assertEquals(cambio, room.lastSwitchChange());
  }

  @Test
  void actualizarEstadoSwitchSinCambioRealConservaElMomentoAnterior() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    siteStateService.getSiteState(inicio);

    siteStateService.updateSwitchState("switch-1", SwitchState.APAGADO, inicio.plusMinutes(30));

    Room room = siteStateService.getSiteState(inicio.plusMinutes(35)).habitaciones().get(0);
    assertEquals(inicio, room.lastSwitchChange());
  }

  @Test
  void actualizarTemperaturaConservaElInicioDeSituacion() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    siteStateService.getSiteState(inicio);

    DataSite actualizado = siteStateService.updateRoomTemperature("termo-1", 18.5);

    assertEquals(inicio, actualizado.habitaciones().get(0).lastSwitchChange());
  }

  @Test
  void cargarEstadoConsultaYMapeaElEstadoDeCadaSwitch() {
    clearInvocations(switchClient);

    List<TemperatureMessage> tempReadings = new ArrayList<>();
    tempReadings.add(
        new TemperatureMessage("0", "termo-2", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
    tempReadings.add(
        new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
    when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

    when(switchClient.consultarEstado(any()))
        .thenReturn(
            new SwitchStateResponse("switch-1", SwitchState.ENCENDIDO, null),
            new SwitchStateResponse("switch-2", SwitchState.APAGADO, null));
    Room segundaRoom =
        new Room(
            "hab-2",
            "Dormitorio",
            20.0,
            1.0,
            "termo-2",
            "casa/dormitorio/temperatura",
            "switch-2",
            "http://localhost",
            Double.NaN,
            SwitchState.APAGADO);

    Room primeraRoom =
        new Room(
            "hab-1",
            "Living",
            21.0,
            1.5,
            "termo-1",
            "casa/living/temperatura",
            "switch-1",
            "http://localhost/switches/1",
            19.0,
            SwitchState.APAGADO);
    DataSite nuevoEstado =
        new DataSite(
            new Site("site-2", "Casa", 5.0, new Fee(null)), List.of(primeraRoom, segundaRoom));
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

  private static final Site SITE = new Site("site-1", "Casa", 5.0, new Fee(null));

  private static Room habitacion(
      String id, double temperaturaEsperada, String idSwitch, String idTermostato) {
    return new Room(
        id,
        "Hab " + id,
        temperaturaEsperada,
        1.0,
        idTermostato,
        "casa/" + id + "/temperatura",
        idSwitch,
        "http://localhost/switches",
        Double.NaN,
        SwitchState.APAGADO);
  }

  @Test
  void recargarSoloConfiguracionNoConsultaSwitchesYConservaElEstadoEnEjecucion() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    siteStateService.loadSiteState(
        new DataSite(SITE, List.of(habitacion("hab-1", 21.0, "switch-1", "termo-1"))));
    siteStateService.getSiteState(inicio);
    siteStateService.updateSwitchState("switch-1", SwitchState.ENCENDIDO, inicio);
    clearInvocations(switchClient, tempReadingsRepository);

    siteStateService.loadSiteState(
        new DataSite(SITE, List.of(habitacion("hab-1", 23.0, "switch-1", "termo-1"))));
    Room room = siteStateService.getSiteState(inicio.plusHours(1)).habitaciones().get(0);

    verify(switchClient, never()).consultarEstado(any());
    verify(tempReadingsRepository, never()).getLastTemperatureMeasurements(any());
    assertEquals(23.0, room.expectedTemperature());
    assertEquals(19.0, room.currentTemperatureC());
    assertEquals(SwitchState.ENCENDIDO, room.currentSwitchState());
    assertEquals(inicio, room.lastSwitchChange());
  }

  @Test
  void reemplazoCompletoOlvidaElEstadoEnEjecucionYVuelveAHidratarTodas() {
    ZonedDateTime inicio = ZonedDateTime.parse("2026-10-24T21:00:00Z");
    Room hab1 = habitacion("hab-1", 21.0, "switch-1", "termo-1");
    siteStateService.loadSiteState(new DataSite(SITE, List.of(hab1)));
    siteStateService.getSiteState(inicio);
    siteStateService.updateSwitchState("switch-1", SwitchState.ENCENDIDO, inicio);
    clearInvocations(switchClient, tempReadingsRepository);

    siteStateService.loadSiteState(new DataSite(SITE, List.of(hab1)), true);
    Room room = siteStateService.getSiteState().habitaciones().get(0);

    verify(switchClient).consultarEstado(hab1);
    verify(tempReadingsRepository).getLastTemperatureMeasurements(List.of("termo-1"));
    assertNull(room.lastSwitchChange());
  }

  @Test
  void anadirHabitacionSoloConsultaElSwitchDeLaNueva() {
    siteStateService.loadSiteState(
        new DataSite(SITE, List.of(habitacion("hab-1", 21.0, "switch-1", "termo-1"))));
    siteStateService.getSiteState();
    clearInvocations(switchClient, tempReadingsRepository);

    Room nueva = habitacion("hab-2", 20.0, "switch-2", "termo-2");
    siteStateService.loadSiteState(
        new DataSite(SITE, List.of(habitacion("hab-1", 21.0, "switch-1", "termo-1"), nueva)));
    siteStateService.getSiteState();

    verify(switchClient, times(1)).consultarEstado(any());
    verify(switchClient).consultarEstado(nueva);
    verify(tempReadingsRepository).getLastTemperatureMeasurements(List.of("termo-2"));
  }

  @Test
  void cambiarElSwitchDeUnaHabitacionLaVuelveAHidratar() {
    siteStateService.loadSiteState(
        new DataSite(SITE, List.of(habitacion("hab-1", 21.0, "switch-1", "termo-1"))));
    siteStateService.getSiteState(ZonedDateTime.parse("2026-10-24T21:00:00Z"));
    clearInvocations(switchClient);

    Room cambiada = habitacion("hab-1", 21.0, "switch-9", "termo-1");
    siteStateService.loadSiteState(new DataSite(SITE, List.of(cambiada)));
    Room room = siteStateService.getSiteState().habitaciones().get(0);

    verify(switchClient).consultarEstado(cambiada);
    assertNull(room.lastSwitchChange());
  }

  @Test
  void unSwitchQueFallaNoImpideHidratarElRestoYSeReintenta() {
    Room ok = habitacion("hab-1", 21.0, "switch-1", "termo-1");
    Room caida = habitacion("hab-2", 20.0, "switch-2", "termo-2");
    when(tempReadingsRepository.getLastTemperatureMeasurements(any()))
        .thenReturn(
            List.of(
                new TemperatureMessage("0", "termo-1", 19.0, 24.0, Instant.now()),
                new TemperatureMessage("0", "termo-2", 18.0, 24.0, Instant.now())));
    when(switchClient.consultarEstado(caida)).thenThrow(new IllegalStateException("timeout"));
    siteStateService.loadSiteState(new DataSite(SITE, List.of(ok, caida)));

    List<Room> primerCiclo = siteStateService.getSiteState().habitaciones();
    assertEquals(List.of("hab-1"), primerCiclo.stream().map(Room::id).toList());

    clearInvocations(switchClient);
    siteStateService.getSiteState();
    verify(switchClient, never()).consultarEstado(ok);
    verify(switchClient).consultarEstado(caida);
  }
}
