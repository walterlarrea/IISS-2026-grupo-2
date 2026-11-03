package com.iiss.sensores.grupo_2.modelos.output;

import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.enums.ActionType;

/** Comando que solicita cambiar el estado de un switch identificado. */
public class CommandSwitch extends Command {
  String idSwitch;
  SwitchState estado;

  /** Crea una orden para asignar el estado indicado al switch. */
  public CommandSwitch(String idSwitch, SwitchState estado) {
    super(ActionType.SWITCH);
    this.idSwitch = idSwitch;
    this.estado = estado;
  }

  /** Devuelve el tipo de acción asociado al cambio de estado. */
  @Override
  public ActionType tipoAccion() {
    return super.tipoAccion();
  }

  /** Devuelve el identificador del switch objetivo. */
  public String idSwitch() {
    return idSwitch;
  }

  /** Devuelve el estado que debe adoptar el switch. */
  public SwitchState estado() {
    return estado;
  }
}
