package com.iiss.sensores.grupo_2.interfaces;

import com.iiss.sensores.grupo_2.enums.ActionType;

/** Contrato común para los comandos que genera el Core. */
public interface ICommand {
  /** Devuelve el tipo de acción representado por este comando. */
  ActionType tipoAccion();
}
