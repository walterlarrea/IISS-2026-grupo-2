package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

public record ComandoControl(String accion, Instant fechaHora, Double factorTiempo) {
}
