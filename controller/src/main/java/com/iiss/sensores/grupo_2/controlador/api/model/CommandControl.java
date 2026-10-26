package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

public record CommandControl(String accion, Instant fechaHora, int factorTiempo) {
}
