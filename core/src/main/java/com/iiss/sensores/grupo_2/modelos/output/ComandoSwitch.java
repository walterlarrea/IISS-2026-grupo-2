package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.enums.TipoAccion;

public class ComandoSwitch  extends Comando {
    String idSwitch;
    EstadoSwitch estado;

    public ComandoSwitch(String idSwitch, EstadoSwitch estado) {
        super(TipoAccion.SWITCH);
        this.idSwitch = idSwitch;
        this.estado = estado;
    }

    @Override
    public TipoAccion tipoAccion() {
        return super.tipoAccion();
    }

    public String idSwitch() {
        return idSwitch;
    }

    public EstadoSwitch estado() {
        return estado;
    }
}
