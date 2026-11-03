package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.ActionType;
import com.iiss.sensores.grupo_2.interfaces.ICommand;

/** Base para los comandos de acción producidos por el Core. */
public abstract class Command implements ICommand {
  private final ActionType actionType;

  /** Crea un comando del tipo de acción indicado. */
  public Command(ActionType actionType) {
    this.actionType = actionType;
  }

  /** Devuelve el tipo de acción de este comando. */
  public ActionType tipoAccion() {
    return this.actionType;
  }
}
