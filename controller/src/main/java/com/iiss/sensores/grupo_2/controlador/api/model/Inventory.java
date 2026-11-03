package com.iiss.sensores.grupo_2.controlador.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Representa la configuración completa del sitio y sus habitaciones. */
public record Inventory(
    @NotBlank @Pattern(regexp = "^2\\.\\d+$") String schemaVersion,
    @NotNull @Valid Sitio sitio,
    @NotNull List<@NotNull @Valid Habitacion> habitaciones) {
  /** Datos generales del sitio y su potencia contratada. */
  public record Sitio(
      @NotBlank String id,
      @NotBlank String nombre,
      @Positive double potenciaContratadaKW,
      @NotNull @Valid Tarifa tarifa) {}

  /** Configuración de tarifas del sitio. */
  public record Tarifa(@NotNull @Valid Franja punta) {}

  /** Intervalo diario de una tarifa, con formato de hora y días de aplicación. */
  public record Franja(
      @NotBlank @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String desde,
      @NotBlank @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String hasta,
      @NotNull Dias dias) {}

  /** Días en los que se aplica la franja tarifaria. */
  public enum Dias {
    HABILES,
    TODOS
  }

  /** Configuración de una habitación y sus dispositivos asociados. */
  public record Habitacion(
      @NotBlank String id,
      @NotBlank @Size(max = 50) String nombre,
      @NotNull Double temperaturaEsperada,
      @Positive Double potenciaKW,
      @NotBlank String idTermostato,
      @NotBlank String topicTermostato,
      @NotBlank String idSwitch,
      @NotBlank String urlSwitch) {}
}
