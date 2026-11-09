package com.iiss.sensores.grupo_2.controlador.engine.client;

import com.iiss.sensores.grupo_2.modelos.input.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchStateResponse;
import com.iiss.sensores.grupo_2.controlador.engine.utils.CustomClock;
import com.iiss.sensores.grupo_2.enums.SwitchState;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Communicates with Api Switch, sends commands and retrieves switch state */
@Service
public class SwitchClient {
  private static final Logger logger = LoggerFactory.getLogger(SwitchClient.class);
  private final RestClient restClient;
  private final String apiKey;

  public SwitchClient(
      @Qualifier("switchRestClient") RestClient restClient,
      @Value("${SWITCHES_API_KEY:}") String apiKey) {
    /** Ejecuta las órdenes contra el servicio de switches que esté configurado. */
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
    String backoffKey = backoffKey("consulta", room);
    checkBackoff(backoffKey, room);
    SwitchResponse response;
    try {
      response = leerEstado(room);
    } catch (RuntimeException e) {
      registerFailure(backoffKey);
      throw e;
    }
    failures.remove(backoffKey);

    SwitchState switchState =
        switch (response.estado()) {
          case ON -> SwitchState.ENCENDIDO;
          case OFF -> SwitchState.APAGADO;
          case DESCONOCIDO -> {
            apagarSwitchDesconocido(room);
            /** yield devuelve el valor del bloque como resultado de la expresión switch */
            yield SwitchState.APAGADO;
          }
        };

    return new SwitchStateResponse(response.idSwitch(), switchState, response.aplicadoEn());
  }

  /** GET del estado del switch, sin backoff ni efectos secundarios */
  private SwitchResponse leerEstado(Room room) {
    var request = restClient.get().uri(URI.create(componerUrlGetSwitch(room)));
    if (!apiKey.isBlank()) {
      request.header("X-API-Key", apiKey);
    }
    SwitchResponse response = request.retrieve().body(SwitchResponse.class);
    if (response == null || response.estado() == null) {
      throw new IllegalStateException("La API de switches devolvió una respuesta sin estado");
    }
    return response;
  }

  /**
   * Un switch puede contestar OK a un OFF sin apagarse. Como un OFF dado por hecho puede hacer
   * pasar la potencia contratada, se confirma con un GET: si el switch sigue en ON el OFF es un
   * fallo (la habitación sigue contando como encendida y se reintenta). Si el GET no se puede hacer
   * (p. ej. un switch que no se deja consultar) se confía en la respuesta del comando.
   */
  private void confirmarApagado(Room room) {
    SwitchResponse actual;
    try {
      actual = leerEstado(room);
    } catch (RuntimeException e) {
      logger.warn(
          "No se pudo confirmar el OFF del switch {}, se toma la respuesta del comando",
          room.idSwitch());
      return;
    }
    if (actual.estado() == EstadoRespuestaSwitch.ON) {
      throw new IllegalStateException(
          "El switch " + room.idSwitch() + " aceptó el OFF pero sigue en ON");
    }
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
    String backoffKey = backoffKey("comando", room);
    checkBackoff(backoffKey, room);
    var request =
        restClient.post().uri(new URI(componerUrlPostSwitch(room))).body(new SwitchRequest(accion));
    if (!apiKey.isBlank()) {
      request.header("X-API-Key", apiKey);
    }
    try {
      SwitchResponse response = request.retrieve().body(SwitchResponse.class);

      /** Un 200 cuyo estado no coincide con la acción pedida (p. ej. un OFF perdido) es un fallo */
      EstadoRespuestaSwitch esperado =
          accion == EstadoAccionSwitch.ON ? EstadoRespuestaSwitch.ON : EstadoRespuestaSwitch.OFF;
      if (response != null && response.estado() != null && response.estado() != esperado) {
        throw new IllegalStateException(
            "El switch "
                + room.idSwitch()
                + " respondió "
                + response.estado()
                + " al comando "
                + accion);
      }
      if (accion == EstadoAccionSwitch.OFF) {
        confirmarApagado(room);
      }
    } catch (RuntimeException e) {
      registerFailure(backoffKey);
      throw e;
    }
    failures.remove(backoffKey);
  }

  /**
   * A switch that fails is not called again for a growing period (BACKOFF_BASE, doubling up to
   * BACKOFF_MAX), so an unreachable or misbehaving switch can't be retried in a tight loop nor slow
   * down every control cycle. Queries and commands are tracked apart, and a change of the room's
   * switch ID or URL starts over. A success clears the failures.
   *
   * <p>The periods are in the control (simulated) time: with an accelerated clock a backoff in real
   * time would leave a switch without retries for many simulated minutes after it recovered. The
   * cap keeps a recovered switch retried within a couple of minutes.
   */
  private static final Duration BACKOFF_BASE = Duration.ofSeconds(30);

  private static final Duration BACKOFF_MAX = Duration.ofMinutes(2);

  private final Map<String, Failure> failures = new ConcurrentHashMap<>();

  /** Control clock, set by the engine while it runs; null while stopped (system clock) */
  private volatile CustomClock clock;

  /**
   * Sets the clock that measures the backoff. Failures are forgotten: they were timed with the
   * previous clock, and a new simulated start may be earlier than the pending retries.
   */
  public void setClock(CustomClock clock) {
    this.clock = clock;
    failures.clear();
  }

  private Instant now() {
    CustomClock current = clock;
    return current == null ? Instant.now() : current.getTime();
  }

  private record Failure(int count, Instant nextAttemptAt) {}

  /** The switch is not being called because it failed recently */
  public static class SwitchInBackoffException extends IllegalStateException {
    public SwitchInBackoffException(String message) {
      super(message);
    }
  }

  /** Forgets every failure: used when the site config changes, so switches are retried at once */
  public void resetBackoff() {
    failures.clear();
  }

  private static String backoffKey(String operation, Room room) {
    return operation + "|" + room.urlSwitch() + "|" + room.idSwitch();
  }

  private void checkBackoff(String key, Room room) {
    Failure failure = failures.get(key);
    if (failure != null && now().isBefore(failure.nextAttemptAt())) {
      throw new SwitchInBackoffException(
          "El switch " + room.idSwitch() + " falló recientemente, se reintentará más tarde");
    }
  }

  private void registerFailure(String key) {
    failures.merge(
        key,
        new Failure(1, now().plus(BACKOFF_BASE)),
        (previous, ignored) -> {
          int count = previous.count() + 1;
          Duration wait = BACKOFF_BASE.multipliedBy(1L << Math.min(count - 1, 10));
          if (wait.compareTo(BACKOFF_MAX) > 0) {
            wait = BACKOFF_MAX;
          }
          return new Failure(count, now().plus(wait));
        });
  }

  private String componerUrlPostSwitch(Room room) {
    /** Construye la URL completa del switch a partir de la información de la habitación */
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
