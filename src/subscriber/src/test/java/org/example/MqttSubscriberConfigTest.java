package org.example;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MqttSubscriberConfigTest {

  @Test
  void appConfigIsSingletonAndLoadsEnvironmentValues() {
    AppConfig firstInstance = AppConfig.getInstance();
    AppConfig secondInstance = AppConfig.getInstance();

    assertSame(firstInstance, secondInstance);

    Map<String, String> environmentValues = firstInstance.getValues();
    assertNotNull(environmentValues);
    assertEquals(System.getenv(), environmentValues);
  }
}
