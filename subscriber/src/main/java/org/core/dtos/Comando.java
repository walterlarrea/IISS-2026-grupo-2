package org.core.dtos;

import org.core.enums.TipoAccion;

public sealed interface Comando
        permits ComandoSwitch {
    TipoAccion accion();
}
