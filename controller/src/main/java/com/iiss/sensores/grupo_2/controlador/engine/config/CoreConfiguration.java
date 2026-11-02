package com.iiss.sensores.grupo_2.controlador.engine.config;

import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates an instance for Core and Spring Boot manages it */
@Configuration
public class CoreConfiguration {

  @Value("${controlador.inercia-termica-c:0.5}")
  private double inerciaTermicaC;

  @Bean
  public ICore core() {
    return CoreFactory.getCore(inerciaTermicaC);
  }
}
