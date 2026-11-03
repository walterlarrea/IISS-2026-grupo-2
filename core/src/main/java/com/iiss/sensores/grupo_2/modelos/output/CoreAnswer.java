package com.iiss.sensores.grupo_2.modelos.output;

import java.util.List;

/** Contiene la lista de comandos calculados por el Core. */
public record CoreAnswer(List<Command> commands) {}
