package com.iiss.sensores.grupo_2.controlador.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.service.SiteStateService;
import com.iiss.sensores.grupo_2.controlador.engine.service.TemperatureControlEngine;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.enums.RushDays;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.modelos.input.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TemperatureControlEngineIntegrationIT {
  /** Simulates API Switch */
  private static WireMockServer switchesApi;

  @Autowired private TemperatureControlEngine controller;

  @Autowired private SiteStateService siteStateService;

  @MockitoBean private TemperatureReadingRepository tempReadings;

  @MockitoBean private InventoryService inventoryService;

  @BeforeAll
  static void startSimulatedApi() {
    switchesApi = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    switchesApi.start();
  }

  @AfterAll
  static void stopSimulatedApi() {
    switchesApi.stop();
  }

  @Test
  void controlTemperatureWhenEnoughPower() {
    /** Simulated APi with each initial state. */
    switchConfig("sw-sim-room1", "OFF");
    switchConfig("sw-sim-room2", "OFF");
    switchConfig("sw-sim-room3", "ON");

    Site site =
        new Site(
            "casa-rodriguez",
            "Casa Rodríguez",
            5.0,
            new Fee(new RushHour("17:00", "23:00", RushDays.HABILES)));

    List<Room> rooms =
        List.of(
            new Room(
                "room1",
                "Living",
                22.0,
                1.2,
                "ht-sim-room1",
                "ht-sim-room1/status/temperature:0",
                "sw-sim-room1",
                switchesApi.baseUrl(),
                18.5,
                SwitchState.APAGADO),
            new Room(
                "room2",
                "Dormitorio principal",
                21.0,
                1.0,
                "ht-sim-room2",
                "ht-sim-room2/status/temperature:0",
                "sw-sim-room2",
                switchesApi.baseUrl(),
                19.0,
                SwitchState.APAGADO),
            new Room(
                "room3",
                "Parrillero",
                21.0,
                1.2,
                "ht-sim-room3",
                "ht-sim-room3/status/temperature:0",
                "sw-sim-room3",
                switchesApi.baseUrl(),
                23.0,
                SwitchState.ENCENDIDO));

    // Initiate state manually.
    siteStateService.loadSiteState(new DataSite(site, rooms));

    when(tempReadings.getLastTemperatureMeasurements(anyList()))
        .thenReturn(
            List.of(
                new TemperatureMessage(
                    "reading1", "ht-sim-room1", 18.5, 65.3, Instant.parse("2026-10-26T11:00:00Z")),
                new TemperatureMessage(
                    "reading2", "ht-sim-room2", 19.0, 66.2, Instant.parse("2026-10-26T11:00:00Z")),
                new TemperatureMessage(
                    "reading3",
                    "ht-sim-room3",
                    23.0,
                    73.4,
                    Instant.parse("2026-10-26T11:00:00Z"))));

    // Out of rush hour.
    controller.start(Instant.parse("2026-10-26T12:00:00Z"), 1);
    controller.execute();

    switchesApi.verify(
        1,
        postRequestedFor(urlEqualTo("/switches/sw-sim-room1/comandos"))
            .withRequestBody(matchingJsonPath("$.accion", equalTo("ON"))));

    switchesApi.verify(
        1,
        postRequestedFor(urlEqualTo("/switches/sw-sim-room2/comandos"))
            .withRequestBody(matchingJsonPath("$.accion", equalTo("ON"))));

    switchesApi.verify(
        1,
        postRequestedFor(urlEqualTo("/switches/sw-sim-room3/comandos"))
            .withRequestBody(matchingJsonPath("$.accion", equalTo("OFF"))));

    DataSite finalState = siteStateService.getSiteState();

    assertEquals(SwitchState.ENCENDIDO, findRoom(finalState, "room1").currentSwitchState());
    assertEquals(SwitchState.ENCENDIDO, findRoom(finalState, "room2").currentSwitchState());
    assertEquals(SwitchState.APAGADO, findRoom(finalState, "room3").currentSwitchState());

    assertTrue(2.2 < 5.0);

    controller.stop();
  }

  private static void switchConfig(String idSwitch, String initialState) {
    switchesApi.stubFor(
        get(urlEqualTo("/switches/" + idSwitch))
            .willReturn(
                okJson(
                    """
                        {
                          "idSwitch": "%s",
                          "estado": "%s",
                          "aplicadoEn": "2026-10-26T00:00:00Z"
                        }
                        """
                        .formatted(idSwitch, initialState))));

    switchesApi.stubFor(
        post(urlEqualTo("/switches/" + idSwitch + "/comandos"))
            .willReturn(
                okJson(
                    """
                        {
                          "idSwitch": "%s",
                          "estado": "%s",
                          "aplicadoEn": "2026-10-26T00:00:00Z"
                        }
                        """
                        .formatted(idSwitch, initialState.equals("ON") ? "ON" : "OFF"))));
  }

  private static Room findRoom(DataSite data, String id) {
    return data.habitaciones().stream()
        .filter(room -> room.id().equals(id))
        .findFirst()
        .orElseThrow();
  }
}
