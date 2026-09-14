package org.example;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class AppConfig {

    public final String MQTT_BROKER_URL = "MQTT_BROKER_URL";

    private final HashMap<String, String> values;

    public AppConfig() {
        this.values = new HashMap<>(System.getenv());
    }

    public String getValue(String key) {
        return this.values.get(key);
    }
}
