package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;

/** Contiene la acción y sus parámetros para controlar el motor. */
public record CommandControl(String accion, Instant fechaHora, int factorTiempo) {}
