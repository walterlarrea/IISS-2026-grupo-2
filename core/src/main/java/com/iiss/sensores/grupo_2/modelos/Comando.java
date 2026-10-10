package com.iiss.sensores.grupo_2.modelos;

import com.iiss.sensores.grupo_2.enums.TipoAccion;

public sealed interface Comando
        permits ComandoSwitch {
    TipoAccion tipoAccion();
}
