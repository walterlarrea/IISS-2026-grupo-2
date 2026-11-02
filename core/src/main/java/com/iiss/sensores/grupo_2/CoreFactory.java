package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.interfaces.ICore;

public class CoreFactory {
  private static final ICore defaultCore = new Core();

  public static ICore getCore() {
    return defaultCore;
  }

  public static ICore getCore(double thermalInertiaBandC) {
    return new Core(thermalInertiaBandC);
  }
}
