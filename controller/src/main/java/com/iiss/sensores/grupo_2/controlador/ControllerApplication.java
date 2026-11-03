package com.iiss.sensores.grupo_2.controlador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Inicia la aplicación Spring del controlador de temperatura. */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class ControllerApplication {
  /** Arranca el servicio del controlador. */
  public static void main(String[] args) {
    SpringApplication.run(ControllerApplication.class, args);
  }
}
