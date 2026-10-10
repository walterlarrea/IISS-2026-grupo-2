package com.iiss.sensores.grupo_2;

public class CoreFactory {
  private static final ICore core = new Core();

  public static ICore getCore() {
    return core;
  }
}
