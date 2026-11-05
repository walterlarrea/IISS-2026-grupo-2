package com.iiss.sensores.grupo_2.controlador.engine.client;

import com.iiss.sensores.grupo_2.modelos.input.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.enums.SwitchState;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

/** Communicates with Api Switch, sends commands and retrieves switch state */
@Service
public class SwitchClient {
  private static final Logger logger = LoggerFactory.getLogger(SwitchClient.class);
  private final RestClient restClient;
  private final String apiKey;

  public SwitchClient(
      @Qualifier("switchRestClient") RestClient restClient,
      @Value("${SWITCHES_API_KEY:}") String apiKey) {
    // ejecuta las órdenes contra el servicio de switches que esté configurado.
    this.restClient = restClient;
    this.apiKey = apiKey;
  }

  public void encender(Room room) throws URISyntaxException {
    comandar(room, EstadoAccionSwitch.ON);
  }

  public void apagar(Room room) throws URISyntaxException {
    comandar(room, EstadoAccionSwitch.OFF);
  }

  public SwitchStateResponse consultarEstado(Room room) {
    var request = restClient.get().uri(URI.create(componerUrlGetSwitch(room)));
    if (!apiKey.isBlank()) {
      request.header("X-API-Key", apiKey);
    }
    SwitchResponse response = request.retrieve().body(SwitchResponse.class);
    if (response == null || response.estado() == null) {
      throw new IllegalStateException("La API de switches devolvió una respuesta sin estado");
    }

    SwitchState switchState =
        switch (response.estado()) {
          case ON -> SwitchState.ENCENDIDO;
          case OFF -> SwitchState.APAGADO;
          case DESCONOCIDO -> {
            apagarSwitchDesconocido(room);
            // yield devuelve el valor del bloque como resultado de la expresión switch
            yield SwitchState.APAGADO;
          }
        };

    return new SwitchStateResponse(response.idSwitch(), switchState, response.aplicadoEn());
  }

  /**
   * Un switch en estado DESCONOCIDO se lleva a un estado conocido enviándole un comando OFF. Si el
   * comando falla se registra el error y se asume APAGADO (el próximo ciclo volverá a decidir).
   */
  private void apagarSwitchDesconocido(Room room) {
    logger.warn(
        "Switch {} reportó estado DESCONOCIDO, se envía comando de apagado", room.idSwitch());
    try {
      apagar(room);
    } catch (Exception e) {
      logger.error(
          "No se pudo apagar el switch {} en estado DESCONOCIDO, url {}",
          room.idSwitch(),
          room.urlSwitch());
    }
  }

  private void comandar(Room room, EstadoAccionSwitch accion) throws URISyntaxException {
    var request =
        restClient.post().uri(new URI(componerUrlPostSwitch(room))).body(new SwitchRequest(accion));
    if (!apiKey.isBlank()) {
      request.header("X-API-Key", apiKey);
    }
    request.retrieve().body(SwitchResponse.class);
  }

  private String componerUrlPostSwitch(Room room) {
    // construye la URL completa del switch a partir de la información de la habitación
    return room.urlSwitch() + "/switches/" + room.idSwitch() + "/comandos";
  }

  private String componerUrlGetSwitch(Room room) {
    return room.urlSwitch() + "/switches/" + room.idSwitch();
  }

  private enum EstadoAccionSwitch {
    ON,
    OFF
  }

  private enum EstadoRespuestaSwitch {
    ON,
    OFF,
    DESCONOCIDO
  }

  private record SwitchRequest(EstadoAccionSwitch accion) {}

  private record SwitchResponse(
      String idSwitch, EstadoRespuestaSwitch estado, Instant aplicadoEn) {}
}
