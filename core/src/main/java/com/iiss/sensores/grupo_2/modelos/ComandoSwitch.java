package com.iiss.sensores.grupo_2.modelos;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.enums.TipoAccion;

public record ComandoSwitch(
        String idSwitch,
        EstadoSwitch estado,
        TipoAccion tipoAccion
)
implements Comando {
    public ComandoSwitch(String idSwitch, EstadoSwitch estado) {
        this(idSwitch, estado, TipoAccion.SWITCH);
    }
//    public ComandoSwitch(String idSwitch, EstadoSwitch estado, TipoAccion tipoAccion) {
//        this(idSwitch, estado, TipoAccion.SWITCH);
//    }
}
