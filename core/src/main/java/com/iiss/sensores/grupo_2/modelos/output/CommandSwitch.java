package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.enums.ActionType;

public class CommandSwitch extends Command {
    String idSwitch;
    SwitchState estado;

    public CommandSwitch(String idSwitch, SwitchState estado) {
        super(ActionType.SWITCH);
        this.idSwitch = idSwitch;
        this.estado = estado;
    }

    @Override
    public ActionType tipoAccion() {
        return super.tipoAccion();
    }

    public String idSwitch() {
        return idSwitch;
    }

    public SwitchState estado() {
        return estado;
    }
}
