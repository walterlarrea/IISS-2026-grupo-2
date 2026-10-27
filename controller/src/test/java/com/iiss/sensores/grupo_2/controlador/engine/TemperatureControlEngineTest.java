package com.iiss.sensores.grupo_2.controlador.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.controlador.engine.service.TemperatureControlEngine;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.RushDays;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemperatureControlEngineTest {
  @Mock private SwitchClient switchClient;
  @Mock private TemperatureReadingRepository tempReadingsRepository;
  @Mock private ICore core;
  private TemperatureControlEngine controller;

  private static class MutableClock extends Clock {
    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant) {
      this.currentInstant = new AtomicReference<>(initialInstant);
      this.zone = ZoneOffset.UTC;
    }

    public void advance(Duration duration) {
      currentInstant.updateAndGet(i -> i.plus(duration));
    }

    @Override
    public ZoneId getZone() {
      return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return currentInstant.get();
    }
  }

  @Test
  void temperaturaActualizadaLlegaAlCore() {
    List<TemperatureMessage> tempReadings = new ArrayList<>();
    tempReadings.add(
        new TemperatureMessage("0", "termo-1", 18.0, 24.0, Instant.parse("2026-10-24T21:09:53Z")));
    when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);

    when(switchClient.consultarEstado(any()))
        .thenReturn(new SwitchStateResponse("switch-1", SwitchState.APAGADO, null));
    SiteStateService siteStateService = new SiteStateService(switchClient, tempReadingsRepository);

    siteStateService.loadSiteState(
        new DataSite(
            new Site("site-1", "Casa", 5.0, new Fee(null)),
            List.of(
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
                    SwitchState.APAGADO))));

    TemperatureMessage tempMsg =
        new TemperatureMessage("0", "termo-1", 18.0, 18.0, Instant.ofEpochMilli(1792846863503L));

    controller = new TemperatureControlEngine(switchClient, core, siteStateService);

    when(core.calcularComandos(any(), any())).thenReturn(new CoreAnswer(List.of()));

    controller.start(Instant.now(), 1);
    controller.processTemperature(tempMsg);

    ArgumentCaptor<DataSite> captor = ArgumentCaptor.forClass(DataSite.class);

    verify(core).calcularComandos(any(), captor.capture());

    assertEquals(18.0, captor.getValue().habitaciones().get(0).currentTemperatureC());
  }

  @Test
  @DisplayName(
      "With timeFactor=10, turns OFF switches of rooms that are still below the expected temperature because rush hour has started")
  void apagaSwitchCuandoEntraEnHoraPuntaConFactorDiez() throws Exception {
    ICore realCore = CoreFactory.getCore();

    // Rush hour period: 17:00 a 23:00 in week days (Wednesday 2026-10-07)
    // 16:59:58 UTC-3 equals to 19:59:58 UTC
    Instant startSimulated = Instant.parse("2026-10-07T19:59:58Z");
    MutableClock mutableClock = new MutableClock(Instant.parse("2026-10-07T10:00:00Z"));

    Room room =
        new Room(
            "hab-1",
            "Living",
            22.0, // Expected temperature
            1.5,
            "termo-1",
            "termo-1/living/temperatura",
            "switch-1",
            "http://localhost/switches/1",
            18.0, // Current temperature (18.0 < 22.0, needs heating)
            SwitchState.ENCENDIDO);

    Site site =
        new Site("site-1", "Casa", 5.0, new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<TemperatureMessage> tempReadings =
        List.of(new TemperatureMessage("0", "termo-1", 18.0, 18.0, startSimulated));
    when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);
    when(switchClient.consultarEstado(any()))
        .thenReturn(new SwitchStateResponse("switch-1", SwitchState.ENCENDIDO, null));

    SiteStateService estadoService = new SiteStateService(switchClient, tempReadingsRepository);
    estadoService.loadSiteState(new DataSite(site, List.of(room)));

    controller = new TemperatureControlEngine(switchClient, realCore, estadoService, mutableClock);

    // timeFactor = 10 (1s real = 10s simulated).
    controller.start(startSimulated, 10);

    // Advances exactly 300ms of base time instantly -> 3000ms simulated (3 seconds)
    // Simulated clock goes from 16:59:58 to 17:00:01 (RUSH HOUR)
    mutableClock.advance(Duration.ofMillis(300));

    controller.execute();

    verify(switchClient, times(1)).apagar(argThat(r -> r.idSwitch().equals("switch-1")));
    verify(switchClient, never()).encender(any());
    assertEquals(
        SwitchState.APAGADO,
        estadoService.getSiteState().habitaciones().get(0).currentSwitchState());
  }

  @Test
  @DisplayName(
      "With timeFactor=20, quickly turns OFF switches of rooms that are still below the expected temperature because rush hour has started")
  void apagaSwitchCuandoEntraEnHoraPuntaConFactorVeinte() throws Exception {
    ICore realCore = CoreFactory.getCore();

    // 16:59:55 UTC-3 equals a 19:59:55 UTC
    Instant startSimulated = Instant.parse("2026-10-07T19:59:55Z");
    MutableClock mutableClock = new MutableClock(Instant.parse("2026-10-07T10:00:00Z"));

    Room room =
        new Room(
            "hab-dormitorio",
            "Dormitorio",
            24.0, // Expected temperature
            2.0,
            "termo-2",
            "termo-2/dormitorio/temperatura",
            "switch-2",
            "http://localhost/switches/2",
            17.5, // Current temperature (17.5 < 24.0, still cold)
            SwitchState.ENCENDIDO);

    Site site =
        new Site(
            "site-2",
            "Casa de Playa",
            4.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<TemperatureMessage> tempReadings =
        List.of(new TemperatureMessage("0", "termo-2", 17.5, 17.5, startSimulated));
    when(tempReadingsRepository.getLastTemperatureMeasurements(any())).thenReturn(tempReadings);
    when(switchClient.consultarEstado(any()))
        .thenReturn(new SwitchStateResponse("switch-2", SwitchState.ENCENDIDO, null));

    SiteStateService siteStateService = new SiteStateService(switchClient, tempReadingsRepository);
    siteStateService.loadSiteState(new DataSite(site, List.of(room)));

    controller =
        new TemperatureControlEngine(switchClient, realCore, siteStateService, mutableClock);

    // timeFactor = 20 (1s real = 20s simulated).
    controller.start(startSimulated, 20);

    // Advances 350ms of base time -> 350 * 20 = 7000ms simulated (7 seconds)
    // The simulated clock goes from 16:59:55 to 17:00:02 (RUSH HOUR)
    mutableClock.advance(Duration.ofMillis(350));

    controller.execute();

    verify(switchClient, times(1)).apagar(argThat(r -> r.idSwitch().equals("switch-2")));
    verify(switchClient, never()).encender(any());
    assertEquals(
        SwitchState.APAGADO,
        siteStateService.getSiteState().habitaciones().get(0).currentSwitchState());
  }
}
