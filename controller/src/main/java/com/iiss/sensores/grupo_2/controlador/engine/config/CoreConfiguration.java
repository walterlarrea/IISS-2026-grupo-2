package com.iiss.sensores.grupo_2.controlador.engine.config;

import com.iiss.sensores.grupo_2.CoreFactory;
import com.iiss.sensores.grupo_2.interfaces.ICore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfiguration {
    @Bean
    public ICore core() {
        return CoreFactory.getCore();
    }
}
