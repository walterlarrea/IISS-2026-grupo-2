package org.example;

import java.util.HashMap;

public class AppConfig {

  public final String MQTT_BROKER_URL = "MQTT_BROKER_URL";
  public final String MQTT_CLIENT_ID = "MQTT_CLIENT_ID";
  public final String MQTT_TOPIC = "MQTT_TOPIC";

  public final String MONGODB_URI = "MONGODB_URI";
  public final String MONGODB_DATABASE = "MONGODB_DATABASE";
  public final String MONGODB_COLLECTION_MEDICIONES = "MONGODB_COLLECTION_MEDICIONES";
  public final String MONGODB_COLLECTION_HABITACIONES = "MONGODB_COLLECTION_HABITACIONES";

  private static final AppConfig INSTANCE = new AppConfig();

  private final HashMap<String, String> values;

  private AppConfig() {
    this.values = new HashMap<>(System.getenv());
  }

  public static AppConfig getInstance() {
    return INSTANCE;
  }

  public HashMap<String, String> getValues() {
    return this.values;
  }
}
