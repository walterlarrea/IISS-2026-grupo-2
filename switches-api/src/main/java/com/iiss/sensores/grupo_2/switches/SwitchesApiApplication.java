package com.iiss.sensores.grupo_2.switches;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Aplicación Spring Boot que expone la API de switches. */
@SpringBootApplication
public class SwitchesApiApplication {
  /** Inicia el servicio web de switches. */
  public static void main(String[] args) {
    SpringApplication.run(SwitchesApiApplication.class, args);
  }
}
