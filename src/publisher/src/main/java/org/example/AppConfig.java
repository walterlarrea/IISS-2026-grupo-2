package org.example;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;

@Singleton
public class AppConfig {

    public final String MQTT_BROKER_URL = "MQTT_BROKER_URL";

    private final HashMap<String, String> values;

    @Inject
    public AppConfig() {
        this.values = new HashMap<>(System.getenv());
    }

    public String getValue(String key) {
        return this.values.get(key);
    }
}
