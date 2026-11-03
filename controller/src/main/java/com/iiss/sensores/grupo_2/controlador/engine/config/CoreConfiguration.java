package com.iiss.sensores.grupo_2.controlador.engine.config;

import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Creates an instance for Core and Spring Boot manages it */
@Configuration
public class CoreConfiguration {

  @Value("${controlador.inercia-termica-c:0.5}")
  private double inerciaTermicaC;

  /** Minutes a room below its target may wait OFF before being served over any other room */
  @Value("${controlador.espera-maxima-min:60}")
  private long esperaMaximaMin;

  /** Minutes a room must stay ON before it can be preempted or yield its power */
  @Value("${controlador.minimo-encendida-min:10}")
  private long minimoEncendidaMin;

  @Bean
  public ICore core() {
    return CoreFactory.getCore(
        inerciaTermicaC, Duration.ofMinutes(esperaMaximaMin), Duration.ofMinutes(minimoEncendidaMin));
  }
}
