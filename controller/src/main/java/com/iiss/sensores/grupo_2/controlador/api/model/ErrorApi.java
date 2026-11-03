package com.iiss.sensores.grupo_2.controlador.api.model;

import java.time.Instant;
import java.util.List;

/** Estructura común de error devuelta por la API REST. */
public record ErrorApi(Instant timestamp, int status, String mensaje, List<String> errores) {}
