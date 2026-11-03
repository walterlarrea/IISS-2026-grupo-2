package com.iiss.sensores.grupo_2.controlador.engine.utils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/** Reloj simulado que permite avanzar el tiempo a un factor configurable. */
public class CustomClock {
  final ZoneId ZONA_MONTEVIDEO = ZoneId.of("America/Montevideo");
  private final Clock baseClock;
  private final Instant startedAt;
  private final Instant startedAtSimulated;
  private final int timeFactor;

  /** Crea un reloj simulado usando el reloj UTC del sistema como base. */
  public CustomClock(Instant startedAtSimulated, int timeFactor) {
    this(startedAtSimulated, timeFactor, Clock.systemUTC());
  }

  /** Crea un reloj simulado usando el reloj base proporcionado. */
  public CustomClock(Instant startedAtSimulated, int timeFactor, Clock baseClock) {
    this.baseClock = Objects.requireNonNull(baseClock, "baseClock cannot be null");
    this.startedAt = baseClock.instant();
    this.startedAtSimulated = startedAtSimulated;
    this.timeFactor = timeFactor;
  }

  /** Devuelve la hora simulada actual como instante UTC. */
  public Instant getTime() {
    long timePassedSinceStarted = baseClock.instant().toEpochMilli() - startedAt.toEpochMilli();
    long timePassedSinceStartedSimulated = timePassedSinceStarted * this.timeFactor;

    return Instant.ofEpochMilli(
        startedAtSimulated.toEpochMilli() + timePassedSinceStartedSimulated);
  }

  /** Devuelve la hora simulada actual en la zona horaria de Montevideo. */
  public ZonedDateTime getTimeZoned() {
    long timePassedSinceStarted = baseClock.instant().toEpochMilli() - startedAt.toEpochMilli();
    long timePassedSinceStartedSimulated = timePassedSinceStarted * this.timeFactor;

    return Instant.ofEpochMilli(startedAtSimulated.toEpochMilli() + timePassedSinceStartedSimulated)
        .atZone(ZONA_MONTEVIDEO);
  }

  /** Devuelve cuándo se inició el reloj real. */
  public Instant getStartedAt() {
    return startedAt;
  }

  /** Devuelve la fecha simulada asignada al iniciar el reloj. */
  public Instant getStartedAtSimulated() {
    return startedAtSimulated;
  }

  /** Devuelve el factor de aceleración del tiempo simulado. */
  public int getTimeFactor() {
    return timeFactor;
  }
}
