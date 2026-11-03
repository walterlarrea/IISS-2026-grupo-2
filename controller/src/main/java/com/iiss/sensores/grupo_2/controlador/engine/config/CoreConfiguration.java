package com.iiss.sensores.grupo_2.controlador.engine.config;

import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el motor Core como dependencia administrada por Spring. */
@Configuration
public class CoreConfiguration {

  @Value("${controlador.inercia-termica-c:0.5}")
  private double inerciaTermicaC;

  /** Crea el Core con la banda de inercia térmica configurada. */
  @Bean
  public ICore core() {
    return CoreFactory.getCore(inerciaTermicaC);
  }
}
