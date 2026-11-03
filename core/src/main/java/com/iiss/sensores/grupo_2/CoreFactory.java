package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.interfaces.ICore;

/** Crea instancias del motor de reglas del dominio. */
public class CoreFactory {
  private static final ICore defaultCore = new Core();

  /** Devuelve la instancia predeterminada del Core. */
  public static ICore getCore() {
    return defaultCore;
  }

  /** Crea un Core configurado con la banda de inercia térmica indicada. */
  public static ICore getCore(double thermalInertiaBandC) {
    return new Core(thermalInertiaBandC);
  }
}
