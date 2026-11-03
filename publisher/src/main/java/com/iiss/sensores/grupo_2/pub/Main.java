package com.iiss.sensores.grupo_2.pub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Inicia la aplicación publicadora de mediciones simuladas. */
@SpringBootApplication
public class Main {
  /** Arranca Spring y comienza a generar eventos de temperatura. */
  public static void main(String[] args) throws InterruptedException {
    EventGenerator eventGenerator =
        SpringApplication.run(Main.class, args).getBean(EventGenerator.class);
    eventGenerator.generate();
  }
}
