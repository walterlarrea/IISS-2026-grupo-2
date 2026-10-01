package org.core;

import org.core.dtos.Comando;
import org.subs.dto.MessageObject.MessageObject;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ICore {
//    List<Comando> iniciar(Instant syncInstant);
//    List<Comando> detener(Instant instant);
//    Las dos funciones de arriba podrían ignorarse y que el estado se controle desde afuera.

    List<Comando> onLecturaDeTemperatura(MessageObject.Temperatura lectura); // TODO: Arreglar el tipo del argumento
    List<Comando> onTick(Instant instant); // TODO: Usamos esto? La idea es que genere decisiones según un momento en el tiempo, no por lectura de temperatura
    List<Comando> onActualizacionDeConfiguracion(Map<String, Object> configuracion, Instant instant); // TODO: Crear un tipo para la configuracion del sitio e.j: (ConfiguracionSitio configuracion)
}
