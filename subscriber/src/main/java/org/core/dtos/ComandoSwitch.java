package org.core.dtos;

import org.core.enums.EstadoSwitch;
import org.core.enums.TipoAccion;

public record ComandoSwitch(
        TipoAccion accion,
        EstadoSwitch estado
)
implements Comando {}
