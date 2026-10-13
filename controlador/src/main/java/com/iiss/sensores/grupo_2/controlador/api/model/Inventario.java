package com.iiss.sensores.grupo_2.controlador.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record Inventario(
        @NotBlank
        @Pattern(regexp = "^2\\.\\d+$")
        String schemaVersion,
        @NotNull @Valid Sitio sitio,
        @NotNull List<@NotNull @Valid Habitacion> habitaciones
) {
    public record Sitio(
            @NotBlank String id,
            @NotBlank String nombre,
            @Positive double potenciaContratadaKW,
            @NotNull @Valid Tarifa tarifa
    ) {
    }

    public record Tarifa(@NotNull @Valid Franja punta) {
    }

    public record Franja(
            @NotBlank
            @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String desde,
            @NotBlank
            @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String hasta,
            @NotNull Dias dias
    ) {
    }

    public enum Dias {
        HABILES,
        TODOS
    }

    public record Habitacion(
            @NotBlank String id,
            @NotBlank @Size(max = 50) String nombre,
            @NotNull Double temperaturaEsperada,
            @Positive double potenciaKW,
            @NotBlank String idTermostato,
            @NotBlank String topicTermostato,
            @NotBlank String idSwitch,
            @NotBlank String urlSwitch
    ) {
    }
}
