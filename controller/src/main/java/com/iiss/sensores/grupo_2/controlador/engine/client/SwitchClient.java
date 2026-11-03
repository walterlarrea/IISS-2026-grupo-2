package com.iiss.sensores.grupo_2.controlador.engine.client;

import com.iiss.sensores.grupo_2.modelos.input.Room;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.enums.SwitchState;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

/** Se comunica con la API de switches para consultar y modificar sus estados. */
@Service
public class SwitchClient {
  private final RestClient restClient;
  private final String apiKey;

  /** Crea el cliente usando la conexión REST y la clave de acceso configuradas. */
  public SwitchClient(
      @Qualifier("switchRestClient") RestClient restClient,
      @Value("${SWITCHES_API_KEY:}") String apiKey) {
    // ejecuta las órdenes contra el servicio de switches que esté configurado.
    this.restClient = restClient;
    this.apiKey = apiKey;
  }

  /** Envía la orden de encender el switch de la habitación. */
  public void encender(Room room) throws URISyntaxException {
    comandar(room, EstadoAccionSwitch.ON);
  }

  /** Envía la orden de apagar el switch de la habitación. */
  public void apagar(Room room) throws URISyntaxException {
    comandar(room, EstadoAccionSwitch.OFF);
  }

  /** Consulta el estado remoto del switch y lo convierte al modelo interno. */
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
        "ON".equals(response.estado())
            ? SwitchState.ENCENDIDO
            : SwitchState.APAGADO;

    return new SwitchStateResponse(response.idSwitch(), switchState, response.aplicadoEn());
  }

  /** Envía una acción al endpoint de comandos del switch. */
  private void comandar(Room room, EstadoAccionSwitch accion) throws URISyntaxException {
    var request =
        restClient.post().uri(new URI(componerUrlPostSwitch(room))).body(new SwitchRequest(accion));
    if (!apiKey.isBlank()) {
      request.header("X-API-Key", apiKey);
    }
    request.retrieve().body(SwitchResponse.class);
  }

  /** Construye la URL para enviar un comando al switch. */
  private String componerUrlPostSwitch(Room room) {
    // construye la URL completa del switch a partir de la información de la habitación
    return room.urlSwitch() + "/switches/" + room.idSwitch() + "/comandos";
  }

  /** Construye la URL para consultar el switch. */
  private String componerUrlGetSwitch(Room room) {
    return room.urlSwitch() + "/switches/" + room.idSwitch();
  }

  /** Acciones admitidas por la API remota de switches. */
  private enum EstadoAccionSwitch {
    ON,
    OFF
  }

  /** Cuerpo JSON enviado al endpoint de comandos del switch. */
  private record SwitchRequest(EstadoAccionSwitch accion) {}

  /** Respuesta JSON recibida de la API remota de switches. */
  private record SwitchResponse(
      String idSwitch, String estado, Instant aplicadoEn) {}
}
