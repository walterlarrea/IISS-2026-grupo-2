package com.iiss.sensores.grupo_2;

import com.iiss.sensores.grupo_2.interfaces.ICore;

public class CoreFactory {
  private static final ICore core = new Core();

  public static ICore getCore() {
    return core;
  }
}
