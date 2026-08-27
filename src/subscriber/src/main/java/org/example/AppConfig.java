package org.example;

import java.util.HashMap;

public class AppConfig {

  public final String MQTT_BROKER_URL = "MQTT_BROKER_URL";
  public final String MQTT_CLIENT_ID = "MQTT_CLIENT_ID";
  public final String MQTT_TOPIC = "MQTT_TOPIC";
//  public final String MQTT_PUBLISHER_ID = "MQTT_PUBLISHER_ID";
//  public final String MQTT_PUBLISHER_ID2 = "MQTT_PUBLISHER_ID2";
//  public final String MQTT_PUBLISHER_ID3 = "MQTT_PUBLISHER_ID3";
//  public final String MQTT_ROOM1 = "MQTT_ROOM1";
//  public final String MQTT_ROOM2 = "MQTT_ROOM2";
//  public final String MQTT_ROOM3 = "MQTT_ROOM3";

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
