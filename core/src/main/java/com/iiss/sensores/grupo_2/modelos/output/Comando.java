package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.TipoAccion;
import com.iiss.sensores.grupo_2.interfaces.IComando;

public abstract class Comando implements IComando {
    private final TipoAccion tipoAccion;

    public Comando(TipoAccion tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public TipoAccion tipoAccion() {
        return this.tipoAccion;
    }
}
