package com.iiss.sensores.grupo_2.pub;

import org.springframework.stereotype.Component;

import java.util.HashMap;

/** Expone la configuración de ejecución obtenida de las variables de entorno. */
@Component
public class AppConfig {

  public final String MQTT_BROKER_URL = "MQTT_BROKER_URL";

  private final HashMap<String, String> values;

  /** Carga las variables de entorno para consultarlas durante la ejecución. */
  public AppConfig() {
    this.values = new HashMap<>(System.getenv());
  }

  /** Devuelve el valor asociado a una clave de configuración. */
  public String getValue(String key) {
    return this.values.get(key);
  }
}
