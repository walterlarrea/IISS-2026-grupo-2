package com.iiss.sensores.grupo_2.controlador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class ControladorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ControladorApplication.class, args);
    }
}
