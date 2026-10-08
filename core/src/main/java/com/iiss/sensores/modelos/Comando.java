package com.iiss.sensores.modelos;

import com.iiss.sensores.enums.TipoAccion;

public sealed interface Comando
        permits ComandoSwitch {
    TipoAccion tipoAccion();
}
