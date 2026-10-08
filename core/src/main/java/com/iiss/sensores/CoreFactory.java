package com.iiss.sensores;

public class CoreFactory {
  private static final ICore core = new Core();

  public static ICore getCore() {
    return core;
  }
}
