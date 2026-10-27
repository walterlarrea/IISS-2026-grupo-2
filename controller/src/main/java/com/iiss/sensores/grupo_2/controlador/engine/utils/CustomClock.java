package com.iiss.sensores.grupo_2.controlador.engine.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CustomClock {
  final ZoneId ZONA_MONTEVIDEO = ZoneId.of("America/Montevideo");
  private final Instant startedAt;
  private final Instant startedAtSimulated;
  private final int timeFactor;

  public CustomClock(Instant startedAtSimulated, int timeFactor) {
    this.startedAt = Instant.now();
    this.startedAtSimulated = startedAtSimulated;
    this.timeFactor = timeFactor;
  }

  public Instant getTime() {
    long timePassedSinceStarted = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
    long timePassedSinceStartedSimulated = timePassedSinceStarted * this.timeFactor;

    return Instant.ofEpochMilli(
        startedAtSimulated.toEpochMilli() + timePassedSinceStartedSimulated);
  }

  public ZonedDateTime getTimeZoned() {
    long timePassedSinceStarted = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
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
