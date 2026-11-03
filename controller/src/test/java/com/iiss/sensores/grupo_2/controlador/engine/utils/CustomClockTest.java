package com.iiss.sensores.grupo_2.controlador.engine.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifica el avance del reloj simulado con distintos factores temporales. */
class CustomClockTest {

  /** Reloj de prueba cuyo instante puede adelantarse manualmente. */
  private static class MutableClock extends Clock {
    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zone;

    /** Crea el reloj mutable con un instante inicial. */
    public MutableClock(Instant initialInstant) {
      this.currentInstant = new AtomicReference<>(initialInstant);
      this.zone = ZoneOffset.UTC;
    }

    /** Avanza el instante actual del reloj de prueba. */
    public void advance(Duration duration) {
      currentInstant.updateAndGet(i -> i.plus(duration));
    }

    /** Devuelve la zona horaria del reloj de prueba. */
    @Override
    public ZoneId getZone() {
      return zone;
    }

    /** Devuelve el reloj de prueba sin modificarlo para la zona solicitada. */
    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    /** Devuelve el instante actual del reloj de prueba. */
    @Override
    public Instant instant() {
      return currentInstant.get();
    }
  }

  /** Comprueba que el factor uno avance en tiempo real. */
  @Test
  @DisplayName("With timeFactor = 1, the simulated time runs at normal speed")
  void tiempoTranscurreNormalConFactorUno() {
    Instant baseReal = Instant.parse("2026-10-01T12:00:00Z");
    Instant baseSimulated = Instant.parse("2026-10-01T10:00:00Z");
    MutableClock mutableClock = new MutableClock(baseReal);

    CustomClock clock = new CustomClock(baseSimulated, 1, mutableClock);

    // Advances 100ms of base time instantly
    mutableClock.advance(Duration.ofMillis(100));

    Instant time = clock.getTime();
    long simulatedPassedMs = Duration.between(baseSimulated, time).toMillis();

    // With timeFactor = 1, real 100ms advance exactly 100 simulated ms
    assertEquals(100, simulatedPassedMs);
    assertEquals(1, clock.getTimeFactor());
    assertEquals(baseSimulated, clock.getStartedAtSimulated());
    assertEquals(baseReal, clock.getStartedAt());
  }

  /** Comprueba que un factor mayor que uno acelere el tiempo y conserve la zona correcta. */
  @Test
  @DisplayName("With timeFactor = 60 (accelerated), the simulated time runs proportionally faster")
  void tiempoTranscurreAceleradoConFactorMayorAUno() {
    Instant baseReal = Instant.parse("2026-10-01T12:00:00Z");
    Instant baseSimulated = Instant.parse("2026-10-01T10:00:00Z");
    MutableClock mutableClock = new MutableClock(baseReal);

    int timeFactor = 60; // 1 real second = 1 simulated minute
    CustomClock clock = new CustomClock(baseSimulated, timeFactor, mutableClock);

    // Advance 50 real ms -> 50 * 60 = 3000ms (3 seconds) simulated
    mutableClock.advance(Duration.ofMillis(50));

    Instant time = clock.getTime();
    long simulatedPassedMs = Duration.between(baseSimulated, time).toMillis();

    assertEquals(3000, simulatedPassedMs);

    ZonedDateTime zonedTime = clock.getTimeZoned();
    assertEquals(ZoneId.of("America/Montevideo"), zonedTime.getZone());
    assertEquals(time.toEpochMilli(), zonedTime.toInstant().toEpochMilli());
    assertEquals(60, clock.getTimeFactor());
  }
}
