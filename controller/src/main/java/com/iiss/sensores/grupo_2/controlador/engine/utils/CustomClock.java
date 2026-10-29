package com.iiss.sensores.grupo_2.controlador.engine.utils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

public class CustomClock {
  final ZoneId ZONA_MONTEVIDEO = ZoneId.of("America/Montevideo");
  private final Clock baseClock;
  private final Instant startedAt;
  private final Instant startedAtSimulated;
  private final int timeFactor;

  public CustomClock(Instant startedAtSimulated, int timeFactor) {
    this(startedAtSimulated, timeFactor, Clock.systemUTC());
  }

  public CustomClock(Instant startedAtSimulated, int timeFactor, Clock baseClock) {
    this.baseClock = Objects.requireNonNull(baseClock, "baseClock cannot be null");
    this.startedAt = baseClock.instant();
    this.startedAtSimulated = startedAtSimulated;
    this.timeFactor = timeFactor;
  }

  public Instant getTime() {
    long timePassedSinceStarted = baseClock.instant().toEpochMilli() - startedAt.toEpochMilli();
    long timePassedSinceStartedSimulated = timePassedSinceStarted * this.timeFactor;

    return Instant.ofEpochMilli(
        startedAtSimulated.toEpochMilli() + timePassedSinceStartedSimulated);
  }

  public ZonedDateTime getTimeZoned() {
    long timePassedSinceStarted = baseClock.instant().toEpochMilli() - startedAt.toEpochMilli();
    long timePassedSinceStartedSimulated = timePassedSinceStarted * this.timeFactor;

    return Instant.ofEpochMilli(startedAtSimulated.toEpochMilli() + timePassedSinceStartedSimulated)
        .atZone(ZONA_MONTEVIDEO);
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getStartedAtSimulated() {
    return startedAtSimulated;
  }

  public int getTimeFactor() {
    return timeFactor;
  }
}
