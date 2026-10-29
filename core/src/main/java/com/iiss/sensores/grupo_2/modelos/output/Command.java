package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.ActionType;
import com.iiss.sensores.grupo_2.interfaces.ICommand;

public abstract class Command implements ICommand {
    private final ActionType actionType;

    public Command(ActionType actionType) {
        this.actionType = actionType;
    }

    public ActionType tipoAccion() {
        return this.actionType;
    }
}
