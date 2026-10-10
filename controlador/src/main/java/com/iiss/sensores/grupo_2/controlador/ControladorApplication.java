package com.iiss.sensores.grupo_2.controlador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ControladorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ControladorApplication.class, args);
    }
}
